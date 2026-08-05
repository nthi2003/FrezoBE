package com.frezo.qtht.job;

import com.frezo.common.scheduling.JobExecutionException;
import com.frezo.common.scheduling.SchedulableJob;
import com.frezo.qtht.service.GoogleDriveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseBackupJob implements SchedulableJob {

    /** jdbc:postgresql://host:port/dbname?params — port và query là tuỳ chọn. */
    private static final Pattern JDBC_POSTGRES = Pattern.compile(
            "jdbc:postgresql://([^/:?]+)(?::(\\d+))?/([^?]+)");

    private static final String DEFAULT_HOST = "localhost";
    private static final String DEFAULT_PORT = "5432";

    private static final String MSG_PG_DUMP_MISSING =
            "Không tìm thấy chương trình sao lưu pg_dump. Hãy cài PostgreSQL client trên máy chủ, "
                    + "hoặc khai báo đường dẫn đầy đủ tới pg_dump trong cấu hình hệ thống "
                    + "(frezo.backup.pg-dump-path).";

    private static final String MSG_DRIVE_MISSING =
            "Chưa cấu hình Google Drive để lưu tệp sao lưu. Hãy đặt tệp credentials service account "
                    + "đúng đường dẫn khai báo tại google.drive.credentials.path.";

    /** Giới hạn ký tự nhật ký pg_dump đưa vào thông báo lỗi — tránh phình cột message. */
    private static final int MAX_OUTPUT_CHARS = 1000;

    private static final long TIMEOUT_MINUTES = 60;

    private final GoogleDriveService googleDriveService;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    /** Mặc định dựa vào PATH; máy chủ Windows thường phải trỏ tuyệt đối tới pg_dump.exe. */
    @Value("${frezo.backup.pg-dump-path:pg_dump}")
    private String pgDumpPath;

    @Override
    public String getCode() {
        return "DB_BACKUP";
    }

    @Override
    public String getDisplayName() {
        return "Sao lưu cơ sở dữ liệu";
    }

    @Override
    public String getDescription() {
        return "Chạy pg_dump toàn bộ database rồi tải file backup lên Google Drive";
    }

    @Override
    public String getModuleCode() {
        return "QTHT";
    }

    @Override
    public String getDefaultCron() {
        return "0 0 12 * * *";
    }

    @Override
    public String checkReadiness() {
        if (resolvePgDumpExecutable() == null) {
            return MSG_PG_DUMP_MISSING;
        }
        if (!googleDriveService.isConfigured()) {
            return MSG_DRIVE_MISSING;
        }
        return null;
    }

    @Override
    public void execute() throws Exception {
        doBackup();
    }

    /**
     * Backup thủ công từ {@code POST /qtht/system/backup} — nuốt lỗi và chỉ log,
     * giữ nguyên hành vi cũ của endpoint (fire-and-forget).
     */
    public void executeBackup() {
        try {
            doBackup();
        } catch (Exception e) {
            log.error("Error during database backup: {}", e.getMessage(), e);
        }
    }

    private void doBackup() throws Exception {
        String pgDump = resolvePgDumpExecutable();
        if (pgDump == null) {
            throw new JobExecutionException(MSG_PG_DUMP_MISSING);
        }
        if (!googleDriveService.isConfigured()) {
            throw new JobExecutionException(MSG_DRIVE_MISSING);
        }

        log.info("Starting database backup using {}", pgDump);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filePath = System.getProperty("java.io.tmpdir") + File.separator
                + "frezo_backup_" + timestamp + ".sql";

        // Ghi log pg_dump ra tệp thay vì đọc pipe: không cần luồng đọc riêng, không sợ đầy buffer.
        File logFile = File.createTempFile("frezo_backup_", ".log");
        ProcessBuilder pb = new ProcessBuilder(
                pgDump,
                "-h", jdbcPart(1, DEFAULT_HOST),
                "-p", jdbcPart(2, DEFAULT_PORT),
                "-U", dbUsername,
                "-f", filePath,
                jdbcPart(3, null));
        pb.environment().put("PGPASSWORD", dbPassword);
        pb.redirectErrorStream(true);
        pb.redirectOutput(logFile);

        int exitCode;
        String output;
        try {
            Process process = pb.start();
            boolean finished = process.waitFor(TIMEOUT_MINUTES, TimeUnit.MINUTES);
            output = readLogTail(logFile);
            if (!finished) {
                process.destroyForcibly();
                throw new JobExecutionException(
                        "Quá trình sao lưu chạy quá " + TIMEOUT_MINUTES + " phút nên đã bị dừng. "
                                + "Hãy kiểm tra kích thước cơ sở dữ liệu và tốc độ ổ đĩa của máy chủ.");
            }
            exitCode = process.exitValue();
        } finally {
            if (!logFile.delete()) {
                logFile.deleteOnExit();
            }
        }

        if (exitCode != 0) {
            throw new JobExecutionException(
                    "Sao lưu cơ sở dữ liệu thất bại (pg_dump trả mã lỗi " + exitCode + "). "
                            + "Thường do sai tài khoản, sai quyền truy cập hoặc không kết nối được máy chủ dữ liệu."
                            + (output.isBlank() ? "" : "\nNhật ký pg_dump: " + output));
        }

        File backupFile = new File(filePath);
        log.info("Database dump created successfully: {}", filePath);
        try {
            googleDriveService.uploadFile(backupFile, "application/sql");
        } catch (Exception e) {
            throw new JobExecutionException(
                    "Đã tạo được tệp sao lưu nhưng tải lên Google Drive thất bại. "
                            + "Hãy kiểm tra credentials và quyền ghi vào thư mục Drive.", e);
        } finally {
            if (backupFile.delete()) {
                log.info("Local backup file deleted.");
            }
        }
    }

    /**
     * Tìm pg_dump chạy được: đường dẫn có dấu phân cách thì kiểm tra trực tiếp, tên trần thì dò PATH.
     * Trả {@code null} khi không tìm thấy — không sinh tiến trình nên gọi được từ màn hình quản trị.
     */
    private String resolvePgDumpExecutable() {
        String configured = (pgDumpPath == null || pgDumpPath.isBlank()) ? "pg_dump" : pgDumpPath.trim();

        if (configured.contains("/") || configured.contains("\\")) {
            File file = new File(configured);
            return file.isFile() && file.canExecute() ? file.getAbsolutePath() : null;
        }

        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isBlank()) {
            return null;
        }
        List<String> names = isWindows()
                ? List.of(configured + ".exe", configured)
                : List.of(configured);
        for (String dir : pathEnv.split(File.pathSeparator)) {
            if (dir.isBlank()) {
                continue;
            }
            for (String name : names) {
                File candidate = new File(dir.trim(), name);
                if (candidate.isFile() && candidate.canExecute()) {
                    return candidate.getAbsolutePath();
                }
            }
        }
        return null;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /** Lấy host (1) / port (2) / tên database (3) từ JDBC URL, dùng mặc định khi URL lạ. */
    private String jdbcPart(int group, String fallback) {
        Matcher matcher = JDBC_POSTGRES.matcher(dbUrl == null ? "" : dbUrl);
        if (matcher.find()) {
            String value = matcher.group(group);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        if (fallback != null) {
            return fallback;
        }
        throw new JobExecutionException(
                "Không đọc được tên cơ sở dữ liệu từ cấu hình kết nối. Hãy kiểm tra lại spring.datasource.url.");
    }

    private static String readLogTail(File logFile) {
        try {
            String output = new String(java.nio.file.Files.readAllBytes(logFile.toPath()), StandardCharsets.UTF_8)
                    .trim();
            return output.length() <= MAX_OUTPUT_CHARS
                    ? output
                    : "…" + output.substring(output.length() - MAX_OUTPUT_CHARS);
        } catch (Exception e) {
            return "";
        }
    }
}
