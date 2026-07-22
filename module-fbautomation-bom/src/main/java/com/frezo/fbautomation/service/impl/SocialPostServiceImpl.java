package com.frezo.fbautomation.service.impl;

import com.frezo.common.service.NotificationService;
import com.frezo.fbautomation.dto.request.SocialPostRequest;
import com.frezo.fbautomation.dto.response.SocialPostResponse;
import com.frezo.fbautomation.entity.SocialPost;
import com.frezo.fbautomation.repository.SocialPostRepository;
import com.frezo.fbautomation.service.SocialPostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

/**
 * SocialPostServiceImpl — quản lý bài viết đa kênh + scheduler.
 * <p>
 * Scheduler chạy mỗi phút, quét SCHEDULED posts tới giờ đăng:
 * - Nếu có Page Access Token (config `frezo.social.fb.page-token`) → publish qua Graph API.
 * - Chưa có token → chỉ notify user + đánh dấu PUBLISHING chờ đăng thủ công.
 * <p>
 * KHÔNG dùng cookie/scraping — chỉ Graph API chính thức để không bị Meta ban.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SocialPostServiceImpl implements SocialPostService {

    private final SocialPostRepository repository;
    private final NotificationService notificationService;

    @Value("${frezo.social.fb.page-token:}")
    private String fbPageToken;

    @Value("${frezo.social.fb.default-page-id:}")
    private String fbDefaultPageId;

    // ============================================================
    //  CRUD
    // ============================================================
    @Override
    @Transactional
    public SocialPostResponse create(SocialPostRequest req) {
        SocialPost post = SocialPost.builder()
                .orgId(req.getOrgId())
                .channel(req.getChannel())
                .targetId(req.getTargetId())
                .targetName(req.getTargetName())
                .title(req.getTitle())
                .content(req.getContent())
                .mediaUrls(req.getMediaUrls())
                .linkUrl(req.getLinkUrl())
                .scheduledAt(req.getScheduledAt())
                .status(req.getScheduledAt() != null ? "SCHEDULED" : "DRAFT")
                .authorUsername(currentUsername())
                .build();
        post = repository.save(post);
        log.info("Created social post {} · channel={} · status={}", post.getId(), post.getChannel(), post.getStatus());
        return toResponse(post);
    }

    @Override
    @Transactional
    public SocialPostResponse update(String id, SocialPostRequest req) {
        SocialPost post = mustFind(id);
        if ("PUBLISHED".equals(post.getStatus())) {
            throw new IllegalStateException("Bài đã publish — không sửa được, hãy tạo bài mới");
        }
        post.setChannel(req.getChannel());
        post.setTargetId(req.getTargetId());
        post.setTargetName(req.getTargetName());
        post.setTitle(req.getTitle());
        post.setContent(req.getContent());
        post.setMediaUrls(req.getMediaUrls());
        post.setLinkUrl(req.getLinkUrl());
        post.setScheduledAt(req.getScheduledAt());
        post.setStatus(req.getScheduledAt() != null ? "SCHEDULED" : "DRAFT");
        return toResponse(repository.save(post));
    }

    @Override
    public SocialPostResponse get(String id) {
        return toResponse(mustFind(id));
    }

    @Override
    @Transactional
    public void delete(String id) {
        SocialPost post = mustFind(id);
        post.setIsDeleted(true);
        repository.save(post);
    }

    @Override
    @Transactional
    public SocialPostResponse duplicate(String id) {
        SocialPost src = mustFind(id);
        SocialPost copy = SocialPost.builder()
                .orgId(src.getOrgId())
                .channel(src.getChannel())
                .targetId(src.getTargetId())
                .targetName(src.getTargetName())
                .title(src.getTitle() != null ? src.getTitle() + " (bản sao)" : null)
                .content(src.getContent())
                .mediaUrls(src.getMediaUrls())
                .linkUrl(src.getLinkUrl())
                .status("DRAFT")
                .authorUsername(currentUsername())
                .build();
        return toResponse(repository.save(copy));
    }

    @Override
    @Transactional
    public SocialPostResponse cancel(String id) {
        SocialPost post = mustFind(id);
        if (!"SCHEDULED".equals(post.getStatus())) {
            throw new IllegalStateException("Chỉ hủy được bài đang SCHEDULED");
        }
        post.setStatus("CANCELLED");
        return toResponse(repository.save(post));
    }

    @Override
    @Transactional
    public SocialPostResponse publishNow(String id) {
        SocialPost post = mustFind(id);
        publishOne(post);
        return toResponse(post);
    }

    @Override
    public List<SocialPostResponse> list(String status, String channel) {
        List<SocialPost> all;
        if (status != null && channel != null) {
            all = repository.findByChannelAndStatus(channel, status);
        } else if (status != null) {
            all = repository.findAll().stream().filter(p -> status.equals(p.getStatus())).toList();
        } else if (channel != null) {
            all = repository.findAll().stream().filter(p -> channel.equals(p.getChannel())).toList();
        } else {
            all = repository.findAll();
        }
        return all.stream()
                .sorted((a, b) -> {
                    var au = a.getCreatedDate(); var bu = b.getCreatedDate();
                    if (au == null && bu == null) return 0;
                    if (au == null) return 1; if (bu == null) return -1;
                    return bu.compareTo(au);
                })
                .map(this::toResponse).toList();
    }

    // ============================================================
    //  SCHEDULER — mỗi phút quét bài tới giờ
    // ============================================================
    @Scheduled(cron = "0 * * * * *") // mỗi phút, giây :00
    @Transactional
    public void runScheduler() {
        List<SocialPost> due = repository.findByStatusAndScheduledAtLessThanEqual("SCHEDULED", OffsetDateTime.now());
        if (due.isEmpty()) return;
        log.info("[SocialPostScheduler] {} bài tới giờ đăng", due.size());
        for (SocialPost p : due) {
            try {
                publishOne(p);
            } catch (Exception ex) {
                log.error("[SocialPostScheduler] Lỗi publish post {}: {}", p.getId(), ex.getMessage(), ex);
                p.setStatus("FAILED");
                p.setErrorMessage(Objects.requireNonNullElse(ex.getMessage(), "Unknown error"));
                p.setRetryCount(Objects.requireNonNullElse(p.getRetryCount(), 0) + 1);
                repository.save(p);
            }
        }
    }

    /**
     * Publish 1 bài. Nếu chưa có Page Token → chuyển sang trạng thái "PUBLISHING" (chờ
     * đăng thủ công) và notify user, không throw exception để scheduler tiếp tục.
     */
    private void publishOne(SocialPost post) {
        boolean hasFbToken   = fbPageToken != null && !fbPageToken.isBlank();
        boolean isFbChannel  = "FACEBOOK_PAGE".equalsIgnoreCase(post.getChannel());

        if (isFbChannel && hasFbToken) {
            // TODO Meta Graph API v20: POST https://graph.facebook.com/v20.0/{page-id}/feed
            //   body: message, access_token, link. Cần permission pages_manage_posts.
            //   Ở đây log placeholder — sẽ implement khi có Page Token thật + App Review.
            log.warn("[SocialPost] Meta Graph API integration chưa implement — bài {} sẽ chờ App Review", post.getId());
            markPublishingManual(post, "Cần Meta App Review — bài viết đã sẵn sàng, hãy copy nội dung và đăng thủ công");
            return;
        }
        // Không có config → luôn chuyển sang "cần đăng thủ công".
        markPublishingManual(post, "Chưa cấu hình " + post.getChannel() + " token — hãy đăng thủ công");
    }

    private void markPublishingManual(SocialPost post, String hint) {
        post.setStatus("PUBLISHING");
        post.setErrorMessage(hint);
        repository.save(post);

        // Notify author để biết bài tới giờ đăng.
        if (post.getAuthorUsername() != null) {
            try {
                notificationService.notify(
                        post.getAuthorUsername(),
                        "Bài viết tới giờ đăng",
                        "Bài '" + trimTitle(post) + "' đã tới giờ — hãy copy nội dung và đăng lên " + post.getChannel(),
                        "SOCIAL_POST_DUE",
                        "SOCIAL_POST",
                        post.getId(),
                        "/mkt/content?post=" + post.getId(),
                        "system",
                        false
                );
            } catch (Exception ex) {
                log.warn("Failed to notify author for post {}: {}", post.getId(), ex.getMessage());
            }
        }
    }

    // ============================================================
    //  HELPERS
    // ============================================================
    private SocialPost mustFind(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy bài viết: " + id));
    }

    private String currentUsername() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            return auth != null ? auth.getName() : "system";
        } catch (Exception ex) { return "system"; }
    }

    private String trimTitle(SocialPost p) {
        String s = p.getTitle();
        if (s == null || s.isBlank()) s = p.getContent();
        if (s == null) return "(không tiêu đề)";
        return s.length() > 60 ? s.substring(0, 60) + "…" : s;
    }

    private SocialPostResponse toResponse(SocialPost p) {
        SocialPostResponse r = new SocialPostResponse();
        r.setId(p.getId());
        r.setOrgId(p.getOrgId());
        r.setChannel(p.getChannel());
        r.setTargetId(p.getTargetId());
        r.setTargetName(p.getTargetName());
        r.setTitle(p.getTitle());
        r.setContent(p.getContent());
        r.setMediaUrls(p.getMediaUrls());
        r.setLinkUrl(p.getLinkUrl());
        r.setScheduledAt(p.getScheduledAt());
        r.setPublishedAt(p.getPublishedAt());
        r.setStatus(p.getStatus());
        r.setExternalId(p.getExternalId());
        r.setExternalUrl(p.getExternalUrl());
        r.setErrorMessage(p.getErrorMessage());
        r.setRetryCount(p.getRetryCount());
        r.setAuthorUsername(p.getAuthorUsername());
        r.setCreatedDate(p.getCreatedDate());
        r.setUpdatedDate(p.getUpdatedDate());
        return r;
    }

    // Fallback default fb page ID for future use (avoid unused-field warning).
    @SuppressWarnings("unused")
    private String defaultPageId() { return fbDefaultPageId; }
}
