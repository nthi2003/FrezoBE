package com.frezo.fbautomation.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;

/**
 * SocialPost — bài viết lên lịch đăng đa kênh (Facebook Page, Zalo OA, Instagram).
 * <p>
 * Workflow chuẩn:
 * <ol>
 *   <li>User soạn bài (nội dung + media) → status = DRAFT</li>
 *   <li>Chọn kênh + thời gian đăng → status = SCHEDULED, scheduledAt = ...</li>
 *   <li>Scheduler service quét mỗi phút, tới giờ thì gọi Graph API / Zalo Send API</li>
 *   <li>Publish OK → status = PUBLISHED + externalId lưu ID bài trên Meta/Zalo</li>
 *   <li>Publish fail → status = FAILED + errorMessage</li>
 * </ol>
 * <p>
 * Chưa có Page Access Token thì bài vẫn được lưu ở SCHEDULED, engine sẽ log warning
 * và không publish — user sẽ thấy trong UI để copy nội dung đăng thủ công.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "social_posts")
public class SocialPost extends BaseEntity {

    /** Org sở hữu bài viết — multi-tenant. */
    @Column(name = "org_id", length = 36)
    private String orgId;

    /** Kênh đăng: FACEBOOK_PAGE / ZALO_OA / INSTAGRAM / TIKTOK. */
    @Column(name = "channel", length = 32, nullable = false)
    private String channel;

    /** ID Page/OA đích trên platform (VD Facebook Page ID). */
    @Column(name = "target_id", length = 128)
    private String targetId;

    /** Tên hiển thị của target (VD "Frezo Official Fanpage") — cache để UI hiển thị nhanh. */
    @Column(name = "target_name", length = 255)
    private String targetName;

    /** Tiêu đề (optional, dùng cho Zalo hoặc IG caption). */
    @Column(name = "title", length = 500)
    private String title;

    /** Nội dung bài viết (Markdown / plain text, tùy platform). */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** URLs media (ảnh/video), JSON array — lưu để publish đa ảnh + gallery. */
    @Column(name = "media_urls", columnDefinition = "TEXT")
    private String mediaUrls;

    /** URL link đính kèm (nếu có) — hiển thị preview khi publish. */
    @Column(name = "link_url", length = 1000)
    private String linkUrl;

    /** Thời gian lên lịch đăng (UTC). */
    @Column(name = "scheduled_at")
    private OffsetDateTime scheduledAt;

    /** Thời gian đã publish thật lên platform. */
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    /** DRAFT | SCHEDULED | PUBLISHING | PUBLISHED | FAILED | CANCELLED. */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    /** ID bài trên platform sau khi publish (VD "123456789_987654321" của FB). */
    @Column(name = "external_id", length = 255)
    private String externalId;

    /** Permalink bài đã publish — dùng để mở tab xem trực tiếp. */
    @Column(name = "external_url", length = 1000)
    private String externalUrl;

    /** Log lỗi nếu publish fail (Graph API error message). */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /** Số lần thử publish (retry logic). */
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    /** User tạo (username). */
    @Column(name = "author_username", length = 100)
    private String authorUsername;
}
