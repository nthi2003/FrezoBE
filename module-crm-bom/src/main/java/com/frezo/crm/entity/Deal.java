package com.frezo.crm.entity;

import com.frezo.common.domain.BaseEntity;
import com.frezo.crm.common.DealStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Cơ hội bán hàng (Sales Opportunity / Deal).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "crm_deal", indexes = {
        @Index(name = "idx_crm_deal_stage", columnList = "stage_id"),
        @Index(name = "idx_crm_deal_owner", columnList = "owner_username"),
        @Index(name = "idx_crm_deal_customer", columnList = "customer_id")
})
public class Deal extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "pipeline_id", nullable = false, length = 36)
    private String pipelineId;

    @Column(name = "stage_id", nullable = false, length = 36)
    private String stageId;

    /** Customer ID (nullable nếu Lead chưa convert). */
    @Column(name = "customer_id", length = 36)
    private String customerId;

    @Column(name = "amount", precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    /** Xác suất chốt (0-100). Có thể override từ stage.probability. */
    @Column(name = "probability")
    private Integer probability;

    @Column(name = "expected_close_date")
    private LocalDate expectedCloseDate;

    @Column(name = "closed_date")
    private LocalDate closedDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DealStatus status;

    @Column(name = "owner_username", length = 50)
    private String ownerUsername;

    @Column(length = 2000)
    private String description;

    /** Nếu bị mất, lưu lý do. */
    @Column(name = "lost_reason", length = 500)
    private String lostReason;
}
