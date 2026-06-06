package com.frezo.qlns.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PayrollResponse {
    private String id;
    private String contractId;
    private String personId;
    private Integer payMonth;
    private Integer payYear;
    private BigDecimal baseSalary;
    private Integer standardDays;
    private Integer workingDays;
    private Integer leavesPaid;
    private Integer leavesUnpaid;
    private Integer totalLateMinutes;
    private BigDecimal latePenalty;
    private BigDecimal overtimePay;
    private BigDecimal bonus;
    private BigDecimal deduction;
    private BigDecimal netSalary;
    private Integer status;
    private String statusLabel;
    private LocalDate paidAt;
    private String note;
}
