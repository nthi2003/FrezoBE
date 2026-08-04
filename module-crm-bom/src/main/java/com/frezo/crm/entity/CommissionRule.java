package com.frezo.crm.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Mức hoa hồng theo từng sale (EU cấu hình).
 * username = {@code *} → mức mặc định toàn hệ thống.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "crm_commission_rule",
        uniqueConstraints = @UniqueConstraint(name = "uk_crm_commission_rule_user", columnNames = {"salesperson_username"}),
        indexes = @Index(name = "idx_crm_commission_rule_user", columnList = "salesperson_username"))
public class CommissionRule extends BaseEntity {

    public static final String DEFAULT_USERNAME = "*";

    @Column(name = "salesperson_username", nullable = false, length = 80)
    private String salespersonUsername;

    /** Phần trăm hoa hồng, vd 5.00 = 5%. */
    @Column(name = "rate_percent", precision = 8, scale = 4, nullable = false)
    private BigDecimal ratePercent;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(length = 500)
    private String note;
}
