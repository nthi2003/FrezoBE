package com.frezo.qlns.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO cho bảng lương — thiết kế theo chuẩn luật lao động Việt Nam:
 * <ul>
 *   <li><b>Bên NLĐ (Người lao động)</b>: BHXH 8% + BHYT 1.5% + BHTN 1% + Thuế TNCN + Đoàn phí + Khấu trừ khác</li>
 *   <li><b>Bên NSDLĐ (Người sử dụng lao động)</b>: BHXH 17.5% + BHYT 3% + BHTN 1% + BHTNLĐ-BNN 0.5% + KCN 2%</li>
 *   <li><b>Thuế TNCN</b>: giảm trừ bản thân 11tr + phụ thuộc 4.4tr/người, biểu thuế lũy tiến 7 bậc</li>
 *   <li><b>Tổng chi phí lao động</b> = Gross + Đóng góp NSDLĐ (bức tranh thật cho HR/CEO)</li>
 * </ul>
 *
 * <p>Các field có tiền tố <code>bhxh/bhyt/bhtn/tax/…</code> là alias tương thích FE (v1.0).
 * Các field ở block "Employer" và "Tax Breakdown" là mới v1.2.
 */
@Data
public class PayrollResponse {
    // ============================================================
    // Metadata
    // ============================================================
    private String id;
    private String contractId;
    private String personId;
    private String personName;            // enriched — tên NV để FE hiển thị ngay
    private String personCode;            // enriched — mã NV
    private String orgName;               // enriched — phòng ban / công ty
    private String payrollPeriodId;
    private Integer payMonth;
    private Integer payYear;
    private String period;                // "MM/yyyy" — tiện FE hiển thị

    // ============================================================
    // Salary base
    // ============================================================
    private BigDecimal baseSalary;        // = basicSalary
    private BigDecimal insuranceSalary;   // Lương đóng BH
    private Integer standardDays;
    private Integer standardWorkingDays;  // alias FE
    private Integer workingDays;
    private BigDecimal actualWorkingDays;
    private Integer leavesPaid;
    private Integer leavesUnpaid;

    // ============================================================
    // Attendance-based
    // ============================================================
    private Integer totalLateMinutes;
    private BigDecimal latePenalty;
    private BigDecimal overtimeHoursNormal;    // OT ngày thường
    private BigDecimal overtimeHoursWeekend;   // OT cuối tuần
    private BigDecimal overtimeHoursHoliday;   // OT lễ tết
    private BigDecimal overtimeHours;          // tổng OT hours (alias)
    private BigDecimal overtimePay;
    private BigDecimal overtimeAmount;         // alias FE
    private BigDecimal workingDaysAmount;      // = salaryPerDay * actualWorkingDays

    // ============================================================
    // Earnings
    // ============================================================
    private BigDecimal allowance;
    private BigDecimal bonus;
    private BigDecimal bonusAmount;            // alias FE
    private String bonusReason;                // = note
    private BigDecimal grossSalary;            // Tổng thu nhập trước khấu trừ

    // ============================================================
    // Deductions — NLĐ đóng
    // ============================================================
    private BigDecimal socialInsurance;        // BHXH 8%
    private BigDecimal healthInsurance;        // BHYT 1.5%
    private BigDecimal unemploymentInsurance;  // BHTN 1%
    private BigDecimal bhxh;                   // alias FE
    private BigDecimal bhyt;                   // alias FE
    private BigDecimal bhtn;                   // alias FE
    private BigDecimal totalInsurance;
    private BigDecimal unionFee;               // Đoàn phí (thường 1% max 10% mức lương cơ sở)
    private BigDecimal taxableIncome;
    private BigDecimal taxIncome;              // Thuế TNCN
    private BigDecimal tax;                    // alias FE
    private BigDecimal advanceDeduction;       // Tạm ứng
    private BigDecimal otherDeductions;
    private BigDecimal otherDeduction;         // alias FE (singular)
    private BigDecimal totalDeductions;
    private BigDecimal totalDeduction;         // alias FE

    // ============================================================
    // Net take-home
    // ============================================================
    private BigDecimal netSalary;
    private BigDecimal totalNet;               // alias FE

    // ============================================================
    // Tax breakdown (chi tiết thuế TNCN theo luật VN)
    // ============================================================
    private Integer dependentCount;            // Số người phụ thuộc
    private BigDecimal personalDeduction;      // Giảm trừ bản thân (11.000.000)
    private BigDecimal dependentDeduction;     // Tổng giảm trừ NPT (4.400.000 × count)
    private List<TaxBracketBreakdown> taxBrackets; // Chi tiết từng bậc

    // ============================================================
    // Insurance breakdown (rates để FE hiển thị %)
    // ============================================================
    private BigDecimal socialInsuranceRate;    // 0.08
    private BigDecimal healthInsuranceRate;    // 0.015
    private BigDecimal unemploymentInsuranceRate; // 0.01
    private BigDecimal maxInsuranceSalary;     // 20 × lương cơ sở

    // ============================================================
    // Employer contribution (bức tranh đầy đủ cho HR/CEO)
    // ============================================================
    private BigDecimal employerSocialInsurance;       // 17.5%
    private BigDecimal employerHealthInsurance;       // 3%
    private BigDecimal employerUnemploymentInsurance; // 1%
    private BigDecimal employerAccidentInsurance;     // 0.5% BHTNLĐ-BNN
    private BigDecimal employerSocialRate;
    private BigDecimal employerHealthRate;
    private BigDecimal employerUnemploymentRate;
    private BigDecimal employerAccidentRate;
    private BigDecimal totalEmployerContribution;     // Sum 4 khoản trên
    private BigDecimal totalCompanyCost;              // Gross + Đóng góp NSDLĐ (tổng chi phí thật)

    // ============================================================
    // Region / minimum wage compliance
    // ============================================================
    private String region;                     // Vùng lương tối thiểu (I|II|III|IV)
    private BigDecimal regionalMinimumWage;    // 4.960.000 vùng I
    private Boolean minimumWageCompliant;      // baseSalary >= regionalMinimumWage

    // ============================================================
    // Status & audit
    // ============================================================
    private Integer status;                    // 0=DRAFT, 1=CONFIRMED, 2=PAID
    private String statusCode;                 // "DRAFT" | "CONFIRMED" | "PAID" — cho FE compare
    private String statusLabel;                // "Bản nháp" | "Đã xác nhận" | "Đã thanh toán"
    private LocalDate paidAt;
    private LocalDate paidDate;                // alias FE
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private String note;

    // ============================================================
    // Chi tiết bậc thuế TNCN
    // ============================================================
    @Data
    public static class TaxBracketBreakdown {
        /** Bậc 1..7 */
        private Integer bracket;
        /** Từ (VND) */
        private BigDecimal fromAmount;
        /** Đến (VND, null = không giới hạn) */
        private BigDecimal toAmount;
        /** Thuế suất (0.05 = 5%) */
        private BigDecimal rate;
        /** Phần thu nhập chịu thuế rơi vào bậc này */
        private BigDecimal taxableInBracket;
        /** Thuế phát sinh ở bậc này */
        private BigDecimal taxInBracket;
    }
}
