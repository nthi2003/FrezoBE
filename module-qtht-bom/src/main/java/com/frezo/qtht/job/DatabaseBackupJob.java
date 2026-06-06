package com.frezo.qtht.job;

import com.frezo.qtht.service.GoogleDriveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@Slf4j
@RequiredArgsConstructor
public class DatabaseBackupJob {

    private final GoogleDriveService googleDriveService;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUsername;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    // Cron expression for 12:00 PM daily
    @Scheduled(cron = "0 0 12 * * *")
    public void executeBackup() {
        log.info("Starting scheduled database backup...");
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "frezo_backup_" + timestamp + ".sql";
        String filePath = System.getProperty("java.io.tmpdir") + File.separator + fileName;

        try {
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

            if (exitCode == 0) {
                log.info("Database dump created successfully: {}", filePath);
                
                File backupFile = new File(filePath);
                googleDriveService.uploadFile(backupFile, "application/sql");
                
                // Cleanup local file after upload
                if (backupFile.delete()) {
                    log.info("Local backup file deleted.");
                }
            } else {
                log.error("pg_dump failed with exit code: {}", exitCode);
            }

        } catch (IOException | InterruptedException e) {
            log.error("Error during database backup: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error during backup upload: {}", e.getMessage(), e);
        }
    }
}
