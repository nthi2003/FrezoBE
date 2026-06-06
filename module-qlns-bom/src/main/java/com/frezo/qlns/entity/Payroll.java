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

    @Column(name = "basic_salary")
    private BigDecimal basicSalary;

    @Column(name = "standard_days")
    private Integer standardDays;

    @Column(name = "working_days")
    private Integer workingDays;

    @Column(name = "leaves_paid")
    private Integer leavesPaid;

    @Column(name = "leaves_unpaid")
    private Integer leavesUnpaid;

    @Column(name = "total_late_minutes")
    private Integer totalLateMinutes;

    @Column(name = "late_penalty")
    private BigDecimal latePenalty;

    @Column(name = "overtime_pay")
    private BigDecimal overtimePay;

    @Column(name = "allowance")
    private BigDecimal allowance;

    @Column(name = "bonus")
    private BigDecimal bonus;

    @Column(name = "deduction")
    private BigDecimal deduction;

    @Column(name = "net_salary")
    private BigDecimal netSalary;

    @Column(name = "status")
    private Integer status; // 0=DRAFT, 1=CONFIRMED, 2=PAID

    @Column(name = "paid_at")
    private LocalDate paidAt;

    @Column(name = "note", length = 500)
    private String note;
}
