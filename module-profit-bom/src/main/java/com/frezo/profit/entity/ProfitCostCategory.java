package com.frezo.profit.entity;

import com.frezo.common.domain.BaseEntity;
import com.frezo.profit.common.CostCategoryCode;
import com.frezo.profit.common.CostType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Configurable cost center / category — multi-market (VN, EU) extensibility.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "profit_cost_category",
        indexes = @Index(name = "idx_profit_cost_cat_market", columnList = "market_code, active"))
public class ProfitCostCategory extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "code", length = 40, nullable = false)
    private CostCategoryCode code;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "name_en", length = 200)
    private String nameEn;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_type", length = 20, nullable = false)
    private CostType costType;

    /** ISO 4217 — VND, EUR, … */
    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    /** VN | EU | GLOBAL */
    @Column(name = "market_code", length = 10, nullable = false)
    private String marketCode;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "note", length = 1000)
    private String note;
}
