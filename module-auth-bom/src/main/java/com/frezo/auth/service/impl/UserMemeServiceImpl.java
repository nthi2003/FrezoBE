package com.frezo.auth.service.impl;

import com.frezo.auth.entity.UserMeme;
import com.frezo.auth.repository.UserMemeRepository;
import com.frezo.auth.service.UserMemeService;
import com.frezo.common.exception.AuthException;
import com.frezo.common.service.MinioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserMemeServiceImpl implements UserMemeService {

    private static final int MAX_MEMES_PER_USER = 30;
    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private final UserMemeRepository userMemeRepository;
    private final MinioService minioService;

    @Override
    @Transactional(readOnly = true)
    public List<UserMeme> listMine(String username) {
        return userMemeRepository.findByUsernameAndIsDeletedFalseOrderByCreatedDateDesc(username);
    }

    @Override
    @Transactional
    public UserMeme upload(String username, MultipartFile file, String title) {
        validateFile(file);

        long count = userMemeRepository.countByUsernameAndIsDeletedFalse(username);
        if (count >= MAX_MEMES_PER_USER) {
            throw new AuthException("Bạn đã đạt giới hạn " + MAX_MEMES_PER_USER + " meme. Xoá bớt để tải thêm.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String objectName = "frezo-user/memes/" + username + "/" + UUID.randomUUID() + extension;
        String fileUrl = minioService.uploadFile(objectName, file);

        UserMeme meme = UserMeme.builder()
                .username(username)
                .title(StringUtils.hasText(title) ? title.trim() : stripExtension(originalFilename))
                .fileName(originalFilename)
                .fileUrl(fileUrl)
                .fileSize(file.getSize())
                .contentType(normalizeContentType(file.getContentType(), extension))
                .build();

        UserMeme saved = userMemeRepository.save(meme);
        log.info("Meme uploaded for user {}: {}", username, objectName);
        return saved;
    }

    @Override
    @Transactional
    public void delete(String username, String memeId) {
        UserMeme meme = userMemeRepository.findById(memeId)
                .orElseThrow(() -> new AuthException("Meme not found"));
        if (!username.equals(meme.getUsername())) {
            throw new AuthException("Không có quyền xoá meme này");
        }
        if (Boolean.TRUE.equals(meme.getIsDeleted())) {
            return;
        }

        if (meme.getFileUrl() != null) {
            try {
                String objectName = minioService.extractObjectName(meme.getFileUrl());
                minioService.deleteFile(objectName);
            } catch (Exception e) {
                log.warn("Failed to delete meme file from MinIO id={}: {}", memeId, e.getMessage());
            }
        }

        meme.softDelete(username);
        userMemeRepository.save(meme);
        log.info("Meme deleted: id={}, user={}", memeId, username);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AuthException("File meme trống");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new AuthException("Meme tối đa 2MB");
        }
        String contentType = normalizeContentType(file.getContentType(), extractExtension(file.getOriginalFilename()));
        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new AuthException("Chỉ hỗ trợ ảnh JPG, PNG, GIF, WebP");
        }
    }

    private static String normalizeContentType(String contentType, String extension) {
        if (StringUtils.hasText(contentType)) {
            return contentType.toLowerCase(Locale.ROOT);
        }
        return switch (extension.toLowerCase(Locale.ROOT)) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            default -> "application/octet-stream";
        };
    }

    private static String extractExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return ".png";
        }
        return filename.substring(filename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
    }

    private static String stripExtension(String filename) {
        if (!StringUtils.hasText(filename)) {
            return "Meme";
        }
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }
}
