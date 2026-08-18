package com.frezo.profit.entity;

import com.frezo.common.domain.BaseEntity;
import com.frezo.profit.common.CostCategoryCode;
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
 * Employee / contract cost allocation — links QLNS person & optional contract to a cost center.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "profit_employee_allocation",
        indexes = {
                @Index(name = "idx_profit_emp_alloc_person", columnList = "person_id"),
                @Index(name = "idx_profit_emp_alloc_cat", columnList = "cost_category_code")
        })
public class ProfitEmployeeAllocation extends BaseEntity {

    @Column(name = "person_id", length = 36, nullable = false)
    private String personId;

    /** Optional link to qlns.contract */
    @Column(name = "contract_id", length = 36)
    private String contractId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_category_code", length = 40, nullable = false)
    private CostCategoryCode costCategoryCode;

    /** Override monthly salary; when null and usePayrollSource=true → pull from QLNS payroll */
    @Column(name = "monthly_salary")
    private Double monthlySalary;

    @Column(name = "use_payroll_source")
    private Boolean usePayrollSource;

    /** Sales KPI — target revenue per month (VNĐ) */
    @Column(name = "revenue_target_monthly")
    private Double revenueTargetMonthly;

    /** Sales commission % on revenue (SALES_VARIABLE only) */
    @Column(name = "commission_pct")
    private Double commissionPct;

    /** Tự tính mục tiêu doanh thu từ HĐ 3P + cấu hình chi phí */
    @Column(name = "auto_calculate_target")
    private Boolean autoCalculateTarget;

    @Column(name = "currency", length = 3, nullable = false)
    private String currency;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "note", length = 500)
    private String note;
}
