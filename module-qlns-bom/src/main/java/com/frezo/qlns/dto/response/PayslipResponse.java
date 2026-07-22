package com.frezo.qlns.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Payslip đầy đủ cho Mobile — 1 shot lấy tất cả section:
 * <ul>
 *   <li>Header: tên NV, phòng ban, kỳ, netSalary, trạng thái</li>
 *   <li>Thu nhập: basic, OT (3 mức), phụ cấp, thưởng</li>
 *   <li>Khấu trừ: BHXH/BHYT/BHTN, thuế TNCN, KPCĐ, tạm ứng, khác</li>
 *   <li>Chuyên cần: ngày công, đi muộn, nghỉ</li>
 *   <li>Confirmation state (đã confirm nhận hay chưa)</li>
 * </ul>
 */
@Data
@Builder
public class PayslipResponse {

    // -------- Header --------
    private String payrollId;
    private String personId;
    private String personName;
    private String personCode;
    private String departmentName;
    private String jobTitle;
    private String contractId;

    private Integer month;
    private Integer year;
    private String periodLabel;         // "Tháng 07/2026"
    private BigDecimal netSalary;
    private Integer status;             // draft/confirmed/paid
    private String statusLabel;
    private LocalDate paidAt;

    // -------- Thu nhập --------
    private EarningsSection earnings;

    // -------- Khấu trừ --------
    private DeductionsSection deductions;

    // -------- Chuyên cần --------
    private AttendanceSection attendance;

    // -------- Chi tiết dòng --------
    /** Danh sách chi tiết PayrollDetail (dòng phụ cấp/khấu trừ tùy biến). */
    private List<PayrollDetailResponse> details;

    // -------- Confirmation --------
    private Boolean confirmed;
    private LocalDateTime confirmedAt;
    private String confirmationNote;

    @Data
    @Builder
    public static class EarningsSection {
        private BigDecimal basicSalary;
        private BigDecimal actualWorkingDays;
        private BigDecimal salaryBySalaryDays;   // basic * (workingDays/standardDays)
        private BigDecimal overtimeNormal;
        private BigDecimal overtimeWeekend;
        private BigDecimal overtimeHoliday;
        private BigDecimal overtimePay;          // tổng OT pay
        private BigDecimal allowance;
        private BigDecimal bonus;
        private BigDecimal grossSalary;
    }

    @Data
    @Builder
    public static class DeductionsSection {
        private BigDecimal socialInsurance;      // 8%
        private BigDecimal healthInsurance;      // 1.5%
        private BigDecimal unemploymentInsurance;// 1%
        private BigDecimal totalInsurance;
        private BigDecimal taxableIncome;
        private BigDecimal taxIncome;            // PIT
        private BigDecimal unionFee;
        private BigDecimal latePenalty;
        private BigDecimal advanceDeduction;
        private BigDecimal otherDeductions;
        private BigDecimal totalDeductions;
    }

    @Data
    @Builder
    public static class AttendanceSection {
        private Integer standardDays;
        private Integer workingDays;
        private BigDecimal actualWorkingDays;
        private Integer leavesPaid;
        private Integer leavesUnpaid;
        private Integer totalLateMinutes;
    }
}
