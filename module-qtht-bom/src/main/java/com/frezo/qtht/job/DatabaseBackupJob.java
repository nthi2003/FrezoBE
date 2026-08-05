package com.frezo.qtht.job;

import com.frezo.common.scheduling.SchedulableJob;
import com.frezo.qtht.service.GoogleDriveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseBackupJob implements SchedulableJob {

    private final GoogleDriveService googleDriveService;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

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
        log.info("Starting database backup...");

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "frezo_backup_" + timestamp + ".sql";
        String filePath = System.getProperty("java.io.tmpdir") + File.separator + fileName;

        // Extract DB name from URL (e.g. jdbc:postgresql://localhost:5432/frezo_db)
        String dbName = dbUrl.substring(dbUrl.lastIndexOf("/") + 1);
        if (dbName.contains("?")) {
            dbName = dbName.substring(0, dbName.indexOf("?"));
        }

        ProcessBuilder pb = new ProcessBuilder(
            "pg_dump",
            "-h", "localhost", // Thường là localhost, nếu khác cần config thêm
            "-U", dbUsername,
            "-f", filePath,
            dbName
        );

        // Set password for pg_dump via environment variable
        pb.environment().put("PGPASSWORD", dbPassword);

        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IllegalStateException("pg_dump thất bại với exit code " + exitCode);
        }

        log.info("Database dump created successfully: {}", filePath);
        File backupFile = new File(filePath);
        googleDriveService.uploadFile(backupFile, "application/sql");

        // Cleanup local file after upload
        if (backupFile.delete()) {
            log.info("Local backup file deleted.");
        }
    }
}
