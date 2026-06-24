package com.frezo.qlns.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PayrollResponse {
    private String id;
    private String contractId;
    private String personId;
    private String payrollPeriodId;
    private Integer payMonth;
    private Integer payYear;
    private BigDecimal baseSalary;
    private BigDecimal insuranceSalary;
    private Integer standardDays;
    private Integer workingDays;
    private BigDecimal actualWorkingDays;
    private Integer leavesPaid;
    private Integer leavesUnpaid;
    private Integer totalLateMinutes;
    private BigDecimal latePenalty;
    private BigDecimal overtimeHoursNormal;
    private BigDecimal overtimeHoursWeekend;
    private BigDecimal overtimeHoursHoliday;
    private BigDecimal overtimePay;
    private BigDecimal allowance;
    private BigDecimal bonus;
    private BigDecimal grossSalary;
    private BigDecimal socialInsurance;
    private BigDecimal healthInsurance;
    private BigDecimal unemploymentInsurance;
    private BigDecimal totalInsurance;
    private BigDecimal taxableIncome;
    private BigDecimal taxIncome;
    private BigDecimal unionFee;
    private BigDecimal advanceDeduction;
    private BigDecimal otherDeductions;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;
    private Integer status;
    private String statusLabel;
    private LocalDate paidAt;
    private String note;
}
