package com.frezo.fbautomation.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * AffiliateLink — link ngắn có gắn UTM cho KOL/Affiliate.
 * <p>
 * Flow:
 * <ol>
 *   <li>Admin tạo link: chọn URL đích + campaign + KOL → hệ thống sinh short slug (VD "kolMinh123")</li>
 *   <li>KOL share link {baseUrl}/r/kolMinh123 trên FB/TikTok/Zalo</li>
 *   <li>Ai click → BE ghi lại `AffiliateClick`, redirect 302 sang targetUrl + append UTM</li>
 *   <li>Khi conversion (order/lead/register), gọi API `/mkt/affiliate/{code}/convert` để đếm</li>
 * </ol>
 * Counter (`clickCount`, `conversionCount`, `revenue`) denorm để dashboard load nhanh.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "affiliate_links",
       indexes = { @Index(name = "idx_affiliate_code", columnList = "code", unique = true) })
public class AffiliateLink extends BaseEntity {

    /** Slug ngắn duy nhất (5-16 ký tự alnum), dùng làm path VD `/r/{code}`. */
    @Column(name = "code", length = 32, nullable = false, unique = true)
    private String code;

    /** URL đích thật, sẽ redirect 302 tới đây. */
    @Column(name = "target_url", length = 2000, nullable = false)
    private String targetUrl;

    /** Chiến dịch — nhóm nhiều link theo campaign để so sánh hiệu quả. */
    @Column(name = "campaign", length = 255)
    private String campaign;

    /** Tên KOL/Partner sở hữu link. */
    @Column(name = "kol_name", length = 255)
    private String kolName;

    /** Thông tin liên hệ (SĐT/Zalo/Email) — dùng để đối soát hoa hồng. */
    @Column(name = "kol_contact", length = 255)
    private String kolContact;

    /** UTM params — append vào target khi redirect. */
    @Column(name = "utm_source",   length = 100) private String utmSource;
    @Column(name = "utm_medium",   length = 100) private String utmMedium;
    @Column(name = "utm_campaign", length = 100) private String utmCampaign;
    @Column(name = "utm_term",     length = 100) private String utmTerm;
    @Column(name = "utm_content",  length = 100) private String utmContent;

    /** % hoa hồng (0.05 = 5%) — dùng khi tính commission sau conversion. */
    @Column(name = "commission_rate", precision = 5, scale = 4)
    private BigDecimal commissionRate;

    /** ACTIVE | PAUSED | EXPIRED. */
    @Column(name = "status", length = 16, nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    /** Ngày hết hạn link — sau ngày này redirect sẽ trả 410 Gone. */
    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    // ---- Denormalized counters (cập nhật khi có click/convert) ----
    @Column(name = "click_count")
    @Builder.Default
    private Long clickCount = 0L;

    @Column(name = "unique_click_count")
    @Builder.Default
    private Long uniqueClickCount = 0L;

    @Column(name = "conversion_count")
    @Builder.Default
    private Long conversionCount = 0L;

    @Column(name = "revenue", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal revenue = BigDecimal.ZERO;

    @Column(name = "commission_paid", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal commissionPaid = BigDecimal.ZERO;

    /** Ghi chú nội bộ. */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
