package com.frezo.qlns.engine;

import com.frezo.qlns.entity.InsuranceConfig;
import com.frezo.qlns.entity.PayrollConfig;
import com.frezo.qlns.entity.TaxConfig;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Component
public class PayrollEngine {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    public PayrollResult calculate(PayrollInput input) {
        PayrollResult result = new PayrollResult();

        result.setBasicSalary(input.getBasicSalary());
        result.setInsuranceSalary(input.getInsuranceSalary());
        result.setStandardDays(input.getStandardDays());
        result.setWorkingDays(input.getWorkingDays());
        result.setActualWorkingDays(input.getActualWorkingDays());

        result.setLeavesPaid(input.getLeavesPaid());
        result.setLeavesUnpaid(input.getLeavesUnpaid());
        result.setTotalLateMinutes(input.getTotalLateMinutes());

        result.setOvertimeHoursNormal(input.getOvertimeHoursNormal());
        result.setOvertimeHoursWeekend(input.getOvertimeHoursWeekend());
        result.setOvertimeHoursHoliday(input.getOvertimeHoursHoliday());

        result.setAllowance(input.getAllowance());
        result.setBonus(input.getBonus());
        result.setAdvanceDeduction(input.getAdvanceDeduction());
        result.setOtherDeductions(input.getOtherDeductions());

        calculateGrossPay(result, input);
        calculateInsurance(result, input);
        calculateUnionFee(result, input);
        calculateTax(result, input);
        calculateNetPay(result);

        return result;
    }

    private void calculateGrossPay(PayrollResult result, PayrollInput input) {
        // Guard ÷0 / null — tránh ArithmeticException → 500 cả calculate-all batch
        BigDecimal basic = nonNull(result.getBasicSalary());
        result.setBasicSalary(basic);
        int standardDays = result.getStandardDays() != null && result.getStandardDays() > 0
                ? result.getStandardDays() : 26;
        result.setStandardDays(standardDays);
        int hoursPerDay = input.getStandardHoursPerDay() != null && input.getStandardHoursPerDay() > 0
                ? input.getStandardHoursPerDay() : 8;

        BigDecimal salaryPerDay = basic
                .divide(BigDecimal.valueOf(standardDays), 10, RoundingMode.HALF_UP);
        BigDecimal salaryPerHour = salaryPerDay
                .divide(BigDecimal.valueOf(hoursPerDay), 10, RoundingMode.HALF_UP);

        BigDecimal actualSalary = salaryPerDay.multiply(result.getActualWorkingDays());
        result.setActualSalary(actualSalary);

        BigDecimal overtimePay = BigDecimal.ZERO;
        PayrollConfig pc = input.getPayrollConfig();
        if (pc != null) {
            overtimePay = overtimePay
                    .add(calculateOvertime(result.getOvertimeHoursNormal(), salaryPerHour, pc.getOvertimeNormalRate()))
                    .add(calculateOvertime(result.getOvertimeHoursWeekend(), salaryPerHour, pc.getOvertimeWeekendRate()))
                    .add(calculateOvertime(result.getOvertimeHoursHoliday(), salaryPerHour, pc.getOvertimeHolidayRate()));
        }
        result.setOvertimePay(overtimePay);

        BigDecimal latePenalty = BigDecimal.ZERO;
        if (pc != null && pc.getLatePenaltyPerMinute() != null) {
            latePenalty = BigDecimal.valueOf(result.getTotalLateMinutes())
                    .multiply(pc.getLatePenaltyPerMinute());
        }
        result.setLatePenalty(latePenalty);

        BigDecimal gross = actualSalary
                .add(overtimePay)
                .add(result.getAllowance())
                .add(result.getBonus())
                .subtract(latePenalty);
        result.setGrossSalary(gross.max(BigDecimal.ZERO));
    }

    private BigDecimal calculateOvertime(BigDecimal hours, BigDecimal salaryPerHour, BigDecimal rate) {
        if (hours == null || rate == null) return BigDecimal.ZERO;
        return salaryPerHour.multiply(hours).multiply(rate).setScale(0, RoundingMode.HALF_UP);
    }

    private void calculateInsurance(PayrollResult result, PayrollInput input) {
        InsuranceConfig ic = input.getInsuranceConfig();
        // Early-return case: giữ ZERO defaults thay vì null → tránh NPE ở calculateNetPay/calculateTax.
        if (ic == null) return;

        BigDecimal insuranceSalary = result.getInsuranceSalary();
        if (ic.getMaxInsuranceSalary() != null && insuranceSalary.compareTo(ic.getMaxInsuranceSalary()) > 0) {
            insuranceSalary = ic.getMaxInsuranceSalary();
        }

        // Guard từng rate — có config nhưng thiếu 1-2 rate không được crash cả pipeline.
        BigDecimal si = insuranceSalary.multiply(nonNull(ic.getSocialInsuranceRate())).setScale(0, RoundingMode.HALF_UP);
        BigDecimal hi = insuranceSalary.multiply(nonNull(ic.getHealthInsuranceRate())).setScale(0, RoundingMode.HALF_UP);
        BigDecimal ui = insuranceSalary.multiply(nonNull(ic.getUnemploymentInsuranceRate())).setScale(0, RoundingMode.HALF_UP);

        result.setSocialInsurance(si);
        result.setHealthInsurance(hi);
        result.setUnemploymentInsurance(ui);
        result.setTotalInsurance(si.add(hi).add(ui));
    }

    private void calculateUnionFee(PayrollResult result, PayrollInput input) {
        PayrollConfig pc = input.getPayrollConfig();
        if (pc == null || pc.getUnionDueRate() == null) return;

        BigDecimal fee = nonNull(result.getGrossSalary()).multiply(pc.getUnionDueRate()).setScale(0, RoundingMode.HALF_UP);
        if (pc.getMaxUnionDue() != null && fee.compareTo(pc.getMaxUnionDue()) > 0) {
            fee = pc.getMaxUnionDue();
        }
        result.setUnionFee(fee);
    }

    private void calculateTax(PayrollResult result, PayrollInput input) {
        List<TaxConfig> taxBrackets = input.getTaxConfigs();
        if (taxBrackets == null || taxBrackets.isEmpty()) return;

        TaxConfig baseConfig = taxBrackets.get(0);

        BigDecimal personalDeduction = nonNull(baseConfig.getPersonalDeduction());
        BigDecimal dependentDeduction = nonNull(baseConfig.getDependentDeduction());
        int dep = input.getDependentCount() != null ? input.getDependentCount() : 0;
        BigDecimal dependentTotal = dependentDeduction.multiply(BigDecimal.valueOf(dep));

        BigDecimal taxableIncome = nonNull(result.getGrossSalary())
                .subtract(nonNull(result.getTotalInsurance()))
                .subtract(personalDeduction)
                .subtract(dependentTotal)
                .subtract(nonNull(result.getUnionFee()))
                .max(BigDecimal.ZERO);

        result.setTaxableIncome(taxableIncome);

        BigDecimal taxAmount = BigDecimal.ZERO;
        for (TaxConfig bracket : taxBrackets) {
            if (bracket.getFromAmount() == null || bracket.getRate() == null) continue;
            if (taxableIncome.compareTo(bracket.getFromAmount()) <= 0) break;

            BigDecimal bracketIncome;
            if (bracket.getToAmount() == null) {
                bracketIncome = taxableIncome.subtract(bracket.getFromAmount());
            } else {
                bracketIncome = taxableIncome.min(bracket.getToAmount()).subtract(bracket.getFromAmount());
            }
            if (bracketIncome.compareTo(BigDecimal.ZERO) > 0) {
                taxAmount = taxAmount.add(bracketIncome.multiply(bracket.getRate()));
            }
        }

        result.setTaxIncome(taxAmount.setScale(0, RoundingMode.HALF_UP));
    }

    private void calculateNetPay(PayrollResult result) {
        BigDecimal totalDeductions = nonNull(result.getTotalInsurance())
                .add(nonNull(result.getTaxIncome()))
                .add(nonNull(result.getUnionFee()))
                .add(nonNull(result.getAdvanceDeduction()))
                .add(nonNull(result.getOtherDeductions()));

        result.setTotalDeductions(totalDeductions);
        result.setNetSalary(nonNull(result.getGrossSalary()).subtract(totalDeductions).max(BigDecimal.ZERO));
    }

    /** Null-safe helper cho phép các nhánh early-return (thiếu config) không làm crash chain tính toán. */
    private static BigDecimal nonNull(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    @Data
    @Builder
    public static class PayrollInput {
        private BigDecimal basicSalary;
        private BigDecimal insuranceSalary;
        private Integer standardDays;
        private Integer standardHoursPerDay;
        private Integer workingDays;
        private BigDecimal actualWorkingDays;
        private Integer leavesPaid;
        private Integer leavesUnpaid;
        private Integer totalLateMinutes;
        private BigDecimal overtimeHoursNormal;
        private BigDecimal overtimeHoursWeekend;
        private BigDecimal overtimeHoursHoliday;
        private BigDecimal allowance;
        private BigDecimal bonus;
        private BigDecimal advanceDeduction;
        private BigDecimal otherDeductions;
        private Integer dependentCount;
        private PayrollConfig payrollConfig;
        private InsuranceConfig insuranceConfig;
        private List<TaxConfig> taxConfigs;
    }

    @Data
    public static class PayrollResult {
        private BigDecimal basicSalary          = BigDecimal.ZERO;
        private BigDecimal insuranceSalary      = BigDecimal.ZERO;
        private Integer    standardDays         = 26;
        private Integer    workingDays          = 0;
        private BigDecimal actualWorkingDays    = BigDecimal.ZERO;
        private Integer    leavesPaid           = 0;
        private Integer    leavesUnpaid         = 0;
        private Integer    totalLateMinutes     = 0;
        private BigDecimal overtimeHoursNormal  = BigDecimal.ZERO;
        private BigDecimal overtimeHoursWeekend = BigDecimal.ZERO;
        private BigDecimal overtimeHoursHoliday = BigDecimal.ZERO;
        private BigDecimal allowance            = BigDecimal.ZERO;
        private BigDecimal bonus                = BigDecimal.ZERO;
        private BigDecimal advanceDeduction     = BigDecimal.ZERO;
        private BigDecimal otherDeductions      = BigDecimal.ZERO;

        // Các chỉ số phát sinh trong pipeline — luôn khởi tạo ZERO để calculateNetPay không NPE
        // khi 1 trong các nhánh (insurance/union/tax) bị early-return do thiếu config.
        private BigDecimal actualSalary            = BigDecimal.ZERO;
        private BigDecimal overtimePay             = BigDecimal.ZERO;
        private BigDecimal latePenalty             = BigDecimal.ZERO;
        private BigDecimal grossSalary             = BigDecimal.ZERO;
        private BigDecimal socialInsurance         = BigDecimal.ZERO;
        private BigDecimal healthInsurance         = BigDecimal.ZERO;
        private BigDecimal unemploymentInsurance   = BigDecimal.ZERO;
        private BigDecimal totalInsurance          = BigDecimal.ZERO;
        private BigDecimal unionFee                = BigDecimal.ZERO;
        private BigDecimal taxableIncome           = BigDecimal.ZERO;
        private BigDecimal taxIncome               = BigDecimal.ZERO;
        private BigDecimal totalDeductions         = BigDecimal.ZERO;
        private BigDecimal netSalary               = BigDecimal.ZERO;
    }
}
