package com.frezo.qlns.entity;

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

import java.math.BigDecimal;

/**
 * Khoản phụ cấp / khấu trừ dùng khi tính lương.
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "hr_payroll_component", indexes = {
        @Index(name = "idx_hr_pay_comp_code", columnList = "code", unique = true)
})
public class PayrollComponent extends BaseEntity {

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    /** INCOME | DEDUCTION */
    @Column(name = "nature", length = 20, nullable = false)
    private String nature;

    /** FULL | PARTIAL | NONE — chỉ áp dụng khi nature = INCOME */
    @Column(name = "taxable_type", length = 20)
    private String taxableType;

    @Column(name = "tax_deductible")
    private Boolean taxDeductible;

    /** FIXED | PERCENT */
    @Column(name = "quota_type", length = 20)
    private String quotaType;

    @Column(name = "quota_value", precision = 18, scale = 4)
    private BigDecimal quotaValue;

    @Column(name = "default_value", precision = 18, scale = 2)
    private BigDecimal defaultValue;

    @Column(name = "activated", nullable = false)
    private Boolean activated = true;
}
