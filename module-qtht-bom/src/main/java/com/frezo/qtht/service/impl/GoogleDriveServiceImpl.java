package com.frezo.qtht.service.impl;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.frezo.qtht.service.GoogleDriveService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
@Slf4j
public class GoogleDriveServiceImpl implements GoogleDriveService {

    @Value("${google.drive.folder.id:}")
    private String folderId;

    @Value("${google.drive.credentials.path:service-account.json}")
    private String credentialsPath;

    /**
     * Kiểm tra xem cấu hình Google Drive (tệp credentials) đã được thiết lập chưa.
     */
    @Override
    public boolean isConfigured() {
        return credentialsPath != null
                && !credentialsPath.isBlank()
                && new java.io.File(credentialsPath).isFile();
    }

    /**
     * Trả về đường dẫn đến tệp Service Account credentials của Google Drive.
     */
    @Override
    public String getCredentialsPath() {
        return credentialsPath;
    }

    /**
     * Upload một tệp lên Google Drive vào thư mục được cấu hình và trả về File ID.
     */
    @Override
    public String uploadFile(java.io.File localFile, String mimeType) throws IOException, GeneralSecurityException {
        Drive driveService = getDriveService();

        File fileMetadata = new File();
        fileMetadata.setName(localFile.getName());
        if (folderId != null && !folderId.isEmpty()) {
            fileMetadata.setParents(Collections.singletonList(folderId));
        }

        FileContent mediaContent = new FileContent(mimeType, localFile);
        File file = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute();

        log.info("Uploaded file to Google Drive. File ID: {}", file.getId());
        return file.getId();
    }

    /**
     * Khởi tạo và trả về đối tượng Drive service từ Service Account credentials.
     */
    private Drive getDriveService() throws IOException, GeneralSecurityException {
        GoogleCredentials credentials = GoogleCredentials.fromStream(new FileInputStream(credentialsPath))
                .createScoped(Collections.singletonList(DriveScopes.DRIVE_FILE));

        return new Drive.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("FTech-Backup")
                .build();
    }
}
