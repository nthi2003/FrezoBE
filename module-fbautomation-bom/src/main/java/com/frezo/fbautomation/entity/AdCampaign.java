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

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * AdCampaign — theo dõi chiến dịch quảng cáo thủ công (MVP không cần Meta Marketing API).
 * User nhập spend / impressions / clicks / leads từ Ads Manager → Frezo tính CTR/CPC/CPL.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "mkt_ad_campaigns")
public class AdCampaign extends BaseEntity {

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    /** FACEBOOK | GOOGLE | TIKTOK | ZALO | OTHER */
    @Column(name = "platform", length = 32, nullable = false)
    @Builder.Default
    private String platform = "FACEBOOK";

    /** AWARENESS | TRAFFIC | LEADS | SALES | ENGAGEMENT */
    @Column(name = "objective", length = 32)
    private String objective;

    /** DRAFT | ACTIVE | PAUSED | ENDED */
    @Column(name = "status", length = 16, nullable = false)
    @Builder.Default
    private String status = "DRAFT";

    @Column(name = "budget", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal budget = BigDecimal.ZERO;

    @Column(name = "spend", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal spend = BigDecimal.ZERO;

    @Column(name = "impressions")
    @Builder.Default
    private Long impressions = 0L;

    @Column(name = "clicks")
    @Builder.Default
    private Long clicks = 0L;

    @Column(name = "leads")
    @Builder.Default
    private Long leads = 0L;

    @Column(name = "revenue", precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal revenue = BigDecimal.ZERO;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    /** ID chiến dịch trên platform (tuỳ chọn). */
    @Column(name = "external_ad_id", length = 128)
    private String externalAdId;

    @Column(name = "landing_url", length = 1000)
    private String landingUrl;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
