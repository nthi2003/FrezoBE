package com.frezo.qlns.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "payroll")
public class Payroll extends BaseEntity {

    @Column(name = "person_id", nullable = false)
    private String personId;

    @Column(name = "contract_id")
    private String contractId;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "payroll_period_id")
    private String payrollPeriodId;

    @Column(name = "basic_salary")
    private BigDecimal basicSalary;

    @Column(name = "insurance_salary")
    private BigDecimal insuranceSalary;

    @Column(name = "standard_days")
    private Integer standardDays;

    @Column(name = "working_days")
    private Integer workingDays;

    @Column(name = "actual_working_days")
    private BigDecimal actualWorkingDays;

    @Column(name = "leaves_paid")
    private Integer leavesPaid;

    @Column(name = "leaves_unpaid")
    private Integer leavesUnpaid;

    @Column(name = "total_late_minutes")
    private Integer totalLateMinutes;

    @Column(name = "late_penalty")
    private BigDecimal latePenalty;

    @Column(name = "overtime_hours_normal")
    private BigDecimal overtimeHoursNormal;

    @Column(name = "overtime_hours_weekend")
    private BigDecimal overtimeHoursWeekend;

    @Column(name = "overtime_hours_holiday")
    private BigDecimal overtimeHoursHoliday;

    @Column(name = "overtime_pay")
    private BigDecimal overtimePay;

    @Column(name = "allowance")
    private BigDecimal allowance;

    @Column(name = "bonus")
    private BigDecimal bonus;

    @Column(name = "gross_salary")
    private BigDecimal grossSalary;

    @Column(name = "social_insurance")
    private BigDecimal socialInsurance;

    @Column(name = "health_insurance")
    private BigDecimal healthInsurance;

    @Column(name = "unemployment_insurance")
    private BigDecimal unemploymentInsurance;

    @Column(name = "total_insurance")
    private BigDecimal totalInsurance;

    @Column(name = "taxable_income")
    private BigDecimal taxableIncome;

    @Column(name = "tax_income")
    private BigDecimal taxIncome;

    @Column(name = "union_fee")
    private BigDecimal unionFee;

    @Column(name = "advance_deduction")
    private BigDecimal advanceDeduction;

    @Column(name = "other_deductions")
    private BigDecimal otherDeductions;

    @Column(name = "total_deductions")
    private BigDecimal totalDeductions;

    @Column(name = "net_salary")
    private BigDecimal netSalary;

    @Column(name = "status")
    private Integer status;

    @Column(name = "paid_at")
    private LocalDate paidAt;

    @Column(name = "note", length = 500)
    private String note;
}
