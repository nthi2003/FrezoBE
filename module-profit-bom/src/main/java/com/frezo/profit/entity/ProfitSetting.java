package com.frezo.profit.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Cấu hình chi phí / biên lợi nhuận theo phạm vi GLOBAL, CATEGORY hoặc PRODUCT.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "profit_settings",
        uniqueConstraints = @UniqueConstraint(name = "uk_profit_settings_scope",
                columnNames = {"scope", "scope_ref_id"}),
        indexes = @Index(name = "idx_profit_settings_scope", columnList = "scope, scope_ref_id"))
public class ProfitSetting extends BaseEntity {

    /** GLOBAL | CATEGORY | PRODUCT */
    @Column(name = "scope", length = 20, nullable = false)
    private String scope;

    /** categoryId hoặc productId; null/blank khi GLOBAL */
    @Column(name = "scope_ref_id", length = 36)
    private String scopeRefId;

    @Column(name = "name", length = 200)
    private String name;

    /** Biên lợi nhuận mục tiêu (%) — tham chiếu khi định giá */
    @Column(name = "profit_margin_pct")
    private Double profitMarginPct;

    /** Chi phí rủi ro (% trên doanh thu) */
    @Column(name = "risk_cost_pct")
    private Double riskCostPct;

    /** Chi phí nhân công / đơn vị bán */
    @Column(name = "labor_cost_per_unit")
    private Double laborCostPerUnit;

    /** Chi phí cố định hàng tháng (VNĐ) */
    @Column(name = "fixed_monthly_overhead")
    private Double fixedMonthlyOverhead;

    /** Mục tiêu sản lượng bán/tháng — phân bổ chi phí cố định */
    @Column(name = "monthly_sales_target_units")
    private Double monthlySalesTargetUnits;

    /** Tỷ lệ giá vốn mặc định (% doanh thu) — dashboard break-even */
    @Column(name = "default_cogs_pct")
    private Double defaultCogsPct;

    /** Hoa hồng bán hàng mặc định (% doanh thu) */
    @Column(name = "sales_commission_pct")
    private Double salesCommissionPct;

    /** Thuế TNDN ước tính (%) — tính lợi nhuận sau thuế */
    @Column(name = "corporate_tax_pct")
    private Double corporateTaxPct;

    /** ISO 4217 — multi-currency ready */
    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "note", length = 1000)
    private String note;
}
