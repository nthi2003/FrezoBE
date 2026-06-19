package com.frezo.common.service;

import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket:frezo-bucket}")
    private String defaultBucket;

    /**
     * Tải file lên MinIO
     *
     * @param objectName Tên file/đường dẫn trên MinIO (VD: username/avatar_temp.png)
     * @param file       File cần tải lên
     * @return URL có thể dùng để xem file
     */
    public String uploadFile(String objectName, MultipartFile file) {
        try {
            createBucketIfNotExist(defaultBucket);
            
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(objectName)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            return getPresignedUrl(objectName);
        } catch (Exception e) {
            log.error("Lỗi khi upload file lên Minio: ", e);
            throw new RuntimeException("Không thể tải file lên hệ thống.");
        }
    }

    /**
     * Tải file lên MinIO (từ java.io.File)
     *
     * @param objectName Tên file/đường dẫn trên MinIO (VD: username/avatar.png)
     * @param file       File cần tải lên
     * @return URL có thể dùng để xem file
     */
    public String uploadFileFromPath(String objectName, File file) {
        try {
            createBucketIfNotExist(defaultBucket);
            
            FileInputStream inputStream = new FileInputStream(file);
            String contentType = getContentType(file.getName());
            
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(objectName)
                            .stream(inputStream, file.length(), -1)
                            .contentType(contentType)
                            .build()
            );

            return getPresignedUrl(objectName);
        } catch (Exception e) {
            log.error("Lỗi khi upload file lên Minio: ", e);
            throw new RuntimeException("Không thể tải file lên hệ thống.");
        }
    }

    /**
     * Lấy URL để xem file tạm thời
     *
     * @param objectName Tên file trên MinIO
     * @return URL
     */
    public String getPresignedUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(defaultBucket)
                            .object(objectName)
                            .expiry(24, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Lỗi khi lấy URL file từ Minio: ", e);
            throw new RuntimeException("Không thể lấy dữ liệu ảnh.");
        }
    }


    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(defaultBucket)
                            .object(objectName)
                            .build()
            );
            log.info("Deleted file from MinIO: {}", objectName);
        } catch (Exception e) {
            log.error("Lỗi khi xóa file khỏi Minio: ", e);
            throw new RuntimeException("Không thể xóa file khỏi hệ thống.");
        }
    }


    public String extractObjectName(String fileUrl) {
        try {
            java.net.URL url = new java.net.URL(fileUrl);
            String path = url.getPath();
            String bucketPath = "/" + defaultBucket + "/";
            if (path.startsWith(bucketPath)) {
                return path.substring(bucketPath.length());
            }
            throw new RuntimeException("Cannot extract object name from URL: " + fileUrl);
        } catch (Exception e) {
            log.error("Lỗi khi parse URL file: ", e);
            throw new RuntimeException("Không thể xác định file cần xóa.");
        }
    }


    private void createBucketIfNotExist(String bucketName) {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            log.error("Lỗi khi kiểm tra/tạo bucket: ", e);
            throw new RuntimeException("Lỗi cấu hình lưu trữ.");
        }
    }

    private String getContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "pdf" -> "application/pdf";
            default -> "application/octet-stream";
        };
    }
}
