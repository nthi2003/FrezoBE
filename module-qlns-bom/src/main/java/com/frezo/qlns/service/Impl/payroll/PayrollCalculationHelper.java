package com.frezo.qlns.service.impl.payroll;

import com.frezo.qlns.engine.PayrollEngine;
import com.frezo.qlns.entity.Payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Helper stateless (POJO — KHÔNG là bean Spring) chứa logic thuần tuý dùng lại nhiều nơi.
 * Không inject dependency — mọi method đều là {@code static}.
 */
public final class PayrollCalculationHelper {

    private PayrollCalculationHelper() {}

    /**
     * Sao chép các trường tính toán từ {@link PayrollEngine.PayrollResult} sang {@link Payroll} entity.
     * <p>
     * Ưu tiên giữ nguyên các field manual (allowance/bonus/advance/otherDed) nếu đã có giá trị,
     * ngược lại lấy từ result.
     */
    public static void applyResultToEntity(Payroll payroll, PayrollEngine.PayrollResult result, String contractId) {
        if (contractId != null) payroll.setContractId(contractId);

        payroll.setBasicSalary(result.getBasicSalary());
        payroll.setInsuranceSalary(result.getInsuranceSalary());
        payroll.setStandardDays(result.getStandardDays());
        payroll.setWorkingDays(result.getWorkingDays());
        payroll.setActualWorkingDays(result.getActualWorkingDays());
        payroll.setLeavesPaid(result.getLeavesPaid());
        payroll.setLeavesUnpaid(result.getLeavesUnpaid());
        payroll.setTotalLateMinutes(result.getTotalLateMinutes());

        payroll.setOvertimeHoursNormal(result.getOvertimeHoursNormal());
        payroll.setOvertimeHoursWeekend(result.getOvertimeHoursWeekend());
        payroll.setOvertimeHoursHoliday(result.getOvertimeHoursHoliday());
        payroll.setOvertimePay(result.getOvertimePay());
        payroll.setLatePenalty(result.getLatePenalty());

        if (payroll.getAllowance() == null) payroll.setAllowance(result.getAllowance());
        if (payroll.getBonus() == null) payroll.setBonus(result.getBonus());

        payroll.setGrossSalary(result.getGrossSalary());
        payroll.setSocialInsurance(result.getSocialInsurance());
        payroll.setHealthInsurance(result.getHealthInsurance());
        payroll.setUnemploymentInsurance(result.getUnemploymentInsurance());
        payroll.setTotalInsurance(result.getTotalInsurance());
        payroll.setUnionFee(result.getUnionFee());
        payroll.setTaxableIncome(result.getTaxableIncome());
        payroll.setTaxIncome(result.getTaxIncome());

        if (payroll.getAdvanceDeduction() == null) payroll.setAdvanceDeduction(result.getAdvanceDeduction());
        if (payroll.getOtherDeductions() == null) payroll.setOtherDeductions(result.getOtherDeductions());

        payroll.setTotalDeductions(result.getTotalDeductions());
        payroll.setNetSalary(result.getNetSalary());
    }

    /**
     * Tính lại {@code totalDeductions}/{@code netSalary} sau khi user cập nhật manual bonus/deduction.
     * Không đụng insurance/tax — coi như đã ổn định từ engine.
     */
    public static void recalculateTotals(Payroll p) {
        BigDecimal salaryPerDay = p.getBasicSalary()
                .divide(BigDecimal.valueOf(p.getStandardDays()), 10, RoundingMode.HALF_UP);
        BigDecimal actualSalary = salaryPerDay.multiply(
                p.getActualWorkingDays() != null
                        ? p.getActualWorkingDays()
                        : BigDecimal.valueOf(p.getWorkingDays()));

        if (p.getGrossSalary() == null) {
            p.setGrossSalary(actualSalary
                    .add(nz(p.getOvertimePay()))
                    .subtract(nz(p.getLatePenalty()))
                    .add(nz(p.getAllowance()))
                    .add(nz(p.getBonus())));
        }
        // Nếu đã có gross thì giữ nguyên — allowance/bonus đã cộng trong lần trước.

        BigDecimal totalDed = BigDecimal.ZERO;
        totalDed = totalDed.add(nz(p.getTotalInsurance()));
        totalDed = totalDed.add(nz(p.getTaxIncome()));
        totalDed = totalDed.add(nz(p.getUnionFee()));
        totalDed = totalDed.add(nz(p.getAdvanceDeduction()));
        totalDed = totalDed.add(nz(p.getOtherDeductions()));
        p.setTotalDeductions(totalDed);
        p.setNetSalary(p.getGrossSalary().subtract(totalDed).max(BigDecimal.ZERO));
    }

    /** Nhân {@code base} với tỉ lệ (đã dạng thập phân, VD 0.08 = 8%), làm tròn về VND. */
    public static BigDecimal multiplyPct(BigDecimal base, BigDecimal rate) {
        if (base == null || rate == null) return BigDecimal.ZERO;
        return base.multiply(rate).setScale(0, RoundingMode.HALF_UP);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
