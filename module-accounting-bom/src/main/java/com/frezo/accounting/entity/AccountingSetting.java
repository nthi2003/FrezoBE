package com.frezo.accounting.entity;

import com.frezo.accounting.common.AccountingStandard;
import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Cấu hình kế toán cho toàn hệ thống (Frezo hiện tại là single-tenant).
 * Chỉ có 1 record duy nhất (singleton by convention).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "acc_setting")
public class AccountingSetting extends BaseEntity {

    /** Chuẩn kế toán áp dụng (TT133 hoặc TT99). */
    @Enumerated(EnumType.STRING)
    @Column(name = "standard", nullable = false, length = 10)
    private AccountingStandard standard;

    /** Tiền tệ mặc định (ISO). Mặc định VND. */
    @Column(name = "base_currency", nullable = false, length = 3)
    private String baseCurrency;

    /** Chiến lược post Payroll → GL: AGGREGATE_PERIOD / PER_DEPT / PER_EMPLOYEE. */
    @Column(name = "payroll_posting_strategy", nullable = false, length = 30)
    private String payrollPostingStrategy;

    // ---- TK mặc định cho auto-post ----
    /** TK chi phí lương (VD 6421 với TT133, 641/642/622 với TT99). */
    @Column(name = "acc_salary_expense", length = 20)
    private String accSalaryExpense;

    /** TK phải trả CBCNV (334). */
    @Column(name = "acc_salary_payable", length = 20)
    private String accSalaryPayable;

    /** TK BHXH phải trả (3383). */
    @Column(name = "acc_bhxh_payable", length = 20)
    private String accBhxhPayable;

    /** TK BHYT phải trả (3384). */
    @Column(name = "acc_bhyt_payable", length = 20)
    private String accBhytPayable;

    /** TK BHTN phải trả (TT133: 3385, TT200/TT99: 3386). */
    @Column(name = "acc_bhtn_payable", length = 20)
    private String accBhtnPayable;

    /** TK thuế TNCN khấu trừ (3335). */
    @Column(name = "acc_pit_payable", length = 20)
    private String accPitPayable;

    /** TK kinh phí công đoàn (3382). */
    @Column(name = "acc_union_fee", length = 20)
    private String accUnionFee;
}
