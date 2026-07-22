package com.frezo.crm.entity;

import com.frezo.common.domain.BaseEntity;
import com.frezo.crm.common.ActivityType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Timeline activity gắn với Deal hoặc Customer.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "crm_deal_activity", indexes = {
        @Index(name = "idx_crm_activity_deal", columnList = "deal_id"),
        @Index(name = "idx_crm_activity_customer", columnList = "customer_id")
})
public class DealActivity extends BaseEntity {

    @Column(name = "deal_id", length = 36)
    private String dealId;

    @Column(name = "customer_id", length = 36)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 20)
    private ActivityType activityType;

    @Column(length = 255)
    private String subject;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "happened_at", nullable = false)
    private LocalDateTime happenedAt;

    @Column(name = "owner_username", length = 50)
    private String ownerUsername;
}
