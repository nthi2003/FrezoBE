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
        BigDecimal salaryPerDay = result.getBasicSalary()
                .divide(BigDecimal.valueOf(result.getStandardDays()), 10, RoundingMode.HALF_UP);
        BigDecimal salaryPerHour = salaryPerDay
                .divide(BigDecimal.valueOf(input.getStandardHoursPerDay()), 10, RoundingMode.HALF_UP);

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
        if (ic == null) return;

        BigDecimal insuranceSalary = result.getInsuranceSalary();
        if (ic.getMaxInsuranceSalary() != null && insuranceSalary.compareTo(ic.getMaxInsuranceSalary()) > 0) {
            insuranceSalary = ic.getMaxInsuranceSalary();
        }

        BigDecimal si = insuranceSalary.multiply(ic.getSocialInsuranceRate()).setScale(0, RoundingMode.HALF_UP);
        BigDecimal hi = insuranceSalary.multiply(ic.getHealthInsuranceRate()).setScale(0, RoundingMode.HALF_UP);
        BigDecimal ui = insuranceSalary.multiply(ic.getUnemploymentInsuranceRate()).setScale(0, RoundingMode.HALF_UP);

        result.setSocialInsurance(si);
        result.setHealthInsurance(hi);
        result.setUnemploymentInsurance(ui);
        result.setTotalInsurance(si.add(hi).add(ui));
    }

    private void calculateUnionFee(PayrollResult result, PayrollInput input) {
        PayrollConfig pc = input.getPayrollConfig();
        if (pc == null || pc.getUnionDueRate() == null) return;

        BigDecimal fee = result.getGrossSalary().multiply(pc.getUnionDueRate()).setScale(0, RoundingMode.HALF_UP);
        if (pc.getMaxUnionDue() != null && fee.compareTo(pc.getMaxUnionDue()) > 0) {
            fee = pc.getMaxUnionDue();
        }
        result.setUnionFee(fee);
    }

    private void calculateTax(PayrollResult result, PayrollInput input) {
        List<TaxConfig> taxBrackets = input.getTaxConfigs();
        if (taxBrackets == null || taxBrackets.isEmpty()) return;

        TaxConfig baseConfig = taxBrackets.get(0);

        BigDecimal personalDeduction = baseConfig.getPersonalDeduction() != null ? baseConfig.getPersonalDeduction() : BigDecimal.ZERO;
        BigDecimal dependentDeduction = baseConfig.getDependentDeduction() != null ? baseConfig.getDependentDeduction() : BigDecimal.ZERO;
        BigDecimal dependentTotal = dependentDeduction.multiply(BigDecimal.valueOf(input.getDependentCount()));

        BigDecimal taxableIncome = result.getGrossSalary()
                .subtract(result.getTotalInsurance())
                .subtract(personalDeduction)
                .subtract(dependentTotal)
                .subtract(result.getUnionFee())
                .max(BigDecimal.ZERO);

        result.setTaxableIncome(taxableIncome);

        BigDecimal taxAmount = BigDecimal.ZERO;
        for (TaxConfig bracket : taxBrackets) {
            if (bracket.getFromAmount() == null) continue;
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
        BigDecimal totalDeductions = result.getTotalInsurance()
                .add(result.getTaxIncome())
                .add(result.getUnionFee())
                .add(result.getAdvanceDeduction() != null ? result.getAdvanceDeduction() : BigDecimal.ZERO)
                .add(result.getOtherDeductions() != null ? result.getOtherDeductions() : BigDecimal.ZERO);

        result.setTotalDeductions(totalDeductions);
        result.setNetSalary(result.getGrossSalary().subtract(totalDeductions).max(BigDecimal.ZERO));
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
        private BigDecimal basicSalary;
        private BigDecimal insuranceSalary;
        private Integer standardDays;
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

        private BigDecimal actualSalary;
        private BigDecimal overtimePay;
        private BigDecimal latePenalty;
        private BigDecimal grossSalary;
        private BigDecimal socialInsurance;
        private BigDecimal healthInsurance;
        private BigDecimal unemploymentInsurance;
        private BigDecimal totalInsurance;
        private BigDecimal unionFee;
        private BigDecimal taxableIncome;
        private BigDecimal taxIncome;
        private BigDecimal totalDeductions;
        private BigDecimal netSalary;
    }
}
