package com.frezo.profit.entity;

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

/**
 * Monthly fixed overhead per cost category (IT, logistics, QC, admin, …).
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "profit_fixed_overhead",
        indexes = @Index(name = "idx_profit_fixed_oh_period", columnList = "year, month, category_id"))
public class ProfitFixedOverhead extends BaseEntity {

    @Column(name = "category_id", length = 36, nullable = false)
    private String categoryId;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "note", length = 500)
    private String note;
}
