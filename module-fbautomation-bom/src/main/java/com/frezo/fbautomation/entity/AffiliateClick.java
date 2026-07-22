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

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * AffiliateClick — log 1 lượt click vào affiliate link.
 * <p>
 * Chỉ dùng cho analytics chi tiết + đối soát khi cần audit. Dashboard hiển thị số
 * lấy từ `AffiliateLink.clickCount` (denormalized) để tránh COUNT(*) trên bảng
 * click có thể rất lớn.
 * <p>
 * Field `converted` được cập nhật khi có conversion match được click theo IP + session
 * trong khoảng 30 ngày (attribution window mặc định).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@Table(name = "affiliate_clicks",
       indexes = {
           @Index(name = "idx_click_link_id",    columnList = "link_id"),
           @Index(name = "idx_click_clicked_at", columnList = "clicked_at"),
           @Index(name = "idx_click_ip",         columnList = "ip"),
       })
public class AffiliateClick extends BaseEntity {

    @Column(name = "link_id", length = 36, nullable = false)
    private String linkId;

    /** Slug đã click — cache để join nhanh. */
    @Column(name = "code", length = 32)
    private String code;

    @Column(name = "clicked_at", nullable = false)
    private OffsetDateTime clickedAt;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "referer", length = 1000)
    private String referer;

    /** Country từ IP (dùng geo-ip lookup — có thể null nếu ko config). */
    @Column(name = "country", length = 8)
    private String country;

    /** Đã match conversion chưa. */
    @Column(name = "converted")
    @Builder.Default
    private Boolean converted = false;

    /** Giá trị đơn hàng khi conversion (dùng tính commission). */
    @Column(name = "conversion_value", precision = 15, scale = 2)
    private BigDecimal conversionValue;

    @Column(name = "converted_at")
    private OffsetDateTime convertedAt;
}
