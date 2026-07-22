package com.frezo.qlns.service.impl.payroll;

import com.frezo.qlns.dto.response.PayrollResponse;
import com.frezo.qlns.entity.InsuranceConfig;
import com.frezo.qlns.entity.Payroll;
import com.frezo.qlns.entity.TaxConfig;
import com.frezo.qlns.mapper.PayrollMapper;
import com.frezo.qlns.repository.EmployeeDependentRepository;
import com.frezo.qlns.repository.InsuranceConfigRepository;
import com.frezo.qlns.repository.TaxConfigRepository;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Enrich {@link PayrollResponse}: bổ sung person info, insurance rates, employer contribution
 * và tax bracket breakdown mà entity {@link Payroll} không lưu trực tiếp.
 * <p>
 * Tách ra khỏi service chính để giảm dep count và giúp reuse cho các endpoint read-only.
 */
@Component
@RequiredArgsConstructor
public class PayrollEnricher {

    private final PayrollMapper payrollMapper;
    private final PersonRepository personRepository;
    private final InsuranceConfigRepository insuranceConfigRepository;
    private final TaxConfigRepository taxConfigRepository;
    private final EmployeeDependentRepository employeeDependentRepository;

    /**
     * Enrich đầy đủ: personName + insurance rates + employer contribution + tax bracket breakdown.
     * Dùng cho getById + write ops (calculate/confirm/pay/bonus).
     */
    public PayrollResponse enrichFull(Payroll payroll) {
        Person person = personRepository.findById(payroll.getPersonId()).orElse(null);
        InsuranceConfig ic = payroll.getYear() != null
                ? insuranceConfigRepository.findByYearAndIsActiveTrue(payroll.getYear()).orElse(null)
                : null;
        List<TaxConfig> taxConfigs = payroll.getYear() != null
                ? taxConfigRepository.findByYearAndIsActiveTrueOrderByBracketOrderAsc(payroll.getYear())
                : List.of();
        int deps = payroll.getPersonId() != null
                ? employeeDependentRepository.findByPersonIdAndIsActiveTrue(payroll.getPersonId()).size()
                : 0;
        return enrichWithContext(payroll, person, ic, taxConfigs, deps);
    }

    /**
     * Overload cho phép caller truyền sẵn context (VD orchestrator vừa tính lương xong đã có
     * Person + config trong tay), tránh query lặp lại.
     */
    public PayrollResponse enrichWithContext(Payroll payroll, Person person,
                                             InsuranceConfig ic, List<TaxConfig> taxConfigs, int depCount) {
        PayrollResponse res = payrollMapper.toResponse(payroll);

        if (person != null) {
            res.setPersonName(person.getName());
            res.setPersonCode(person.getCode());
        }
        applyInsuranceInfo(res, payroll, ic);
        applyTaxBreakdown(res, payroll, taxConfigs, depCount);
        return res;
    }

    /**
     * Light enrichment cho list view — chỉ điền person name/code, KHÔNG tính employer contribution
     * hay tax bracket để tránh N+1 query.
     */
    public PayrollResponse enrichLight(Payroll payroll) {
        PayrollResponse res = payrollMapper.toResponse(payroll);
        if (payroll.getPersonId() != null) {
            Optional<Person> personOpt = personRepository.findById(payroll.getPersonId());
            personOpt.ifPresent(p -> {
                res.setPersonName(p.getName());
                res.setPersonCode(p.getCode());
            });
        }
        return res;
    }

    // ============================================================
    // Private helpers
    // ============================================================

    private void applyInsuranceInfo(PayrollResponse res, Payroll payroll, InsuranceConfig ic) {
        if (ic == null) return;

        BigDecimal insuranceBase = payroll.getInsuranceSalary() != null
                ? payroll.getInsuranceSalary() : BigDecimal.ZERO;
        // Cap trần lương đóng bảo hiểm
        if (ic.getMaxInsuranceSalary() != null
                && insuranceBase.compareTo(ic.getMaxInsuranceSalary()) > 0) {
            insuranceBase = ic.getMaxInsuranceSalary();
        }

        res.setSocialInsuranceRate(ic.getSocialInsuranceRate());
        res.setHealthInsuranceRate(ic.getHealthInsuranceRate());
        res.setUnemploymentInsuranceRate(ic.getUnemploymentInsuranceRate());
        res.setMaxInsuranceSalary(ic.getMaxInsuranceSalary());
        res.setEmployerSocialRate(ic.getEmployerSocialRate());
        res.setEmployerHealthRate(ic.getEmployerHealthRate());
        res.setEmployerUnemploymentRate(ic.getEmployerUnemploymentRate());
        res.setEmployerAccidentRate(ic.getEmployerAccidentRate());

        BigDecimal empSocial = PayrollCalculationHelper.multiplyPct(insuranceBase, ic.getEmployerSocialRate());
        BigDecimal empHealth = PayrollCalculationHelper.multiplyPct(insuranceBase, ic.getEmployerHealthRate());
        BigDecimal empUnemp = PayrollCalculationHelper.multiplyPct(insuranceBase, ic.getEmployerUnemploymentRate());
        BigDecimal empAccident = PayrollCalculationHelper.multiplyPct(insuranceBase, ic.getEmployerAccidentRate());
        BigDecimal totalEmp = empSocial.add(empHealth).add(empUnemp).add(empAccident);

        res.setEmployerSocialInsurance(empSocial);
        res.setEmployerHealthInsurance(empHealth);
        res.setEmployerUnemploymentInsurance(empUnemp);
        res.setEmployerAccidentInsurance(empAccident);
        res.setTotalEmployerContribution(totalEmp);

        BigDecimal gross = payroll.getGrossSalary() != null ? payroll.getGrossSalary() : BigDecimal.ZERO;
        res.setTotalCompanyCost(gross.add(totalEmp));

        res.setRegion(ic.getRegion());
        res.setRegionalMinimumWage(ic.getRegionalMinimumWage());
        if (ic.getRegionalMinimumWage() != null && payroll.getBasicSalary() != null) {
            res.setMinimumWageCompliant(
                    payroll.getBasicSalary().compareTo(ic.getRegionalMinimumWage()) >= 0);
        }
    }

    private void applyTaxBreakdown(PayrollResponse res, Payroll payroll,
                                   List<TaxConfig> taxConfigs, int depCount) {
        res.setDependentCount(depCount);
        if (taxConfigs == null || taxConfigs.isEmpty()) return;

        TaxConfig base = taxConfigs.get(0);
        BigDecimal personalDed = base.getPersonalDeduction() != null
                ? base.getPersonalDeduction() : BigDecimal.ZERO;
        BigDecimal depDedUnit = base.getDependentDeduction() != null
                ? base.getDependentDeduction() : BigDecimal.ZERO;
        BigDecimal depDedTotal = depDedUnit.multiply(BigDecimal.valueOf(depCount));

        res.setPersonalDeduction(personalDed);
        res.setDependentDeduction(depDedTotal);

        BigDecimal taxable = payroll.getTaxableIncome();
        if (taxable == null || taxable.compareTo(BigDecimal.ZERO) <= 0) return;

        List<PayrollResponse.TaxBracketBreakdown> breakdown = new ArrayList<>();
        for (TaxConfig b : taxConfigs) {
            if (b.getFromAmount() == null || b.getRate() == null) continue;
            if (taxable.compareTo(b.getFromAmount()) <= 0) break;

            BigDecimal bracketIncome = (b.getToAmount() == null)
                    ? taxable.subtract(b.getFromAmount())
                    : taxable.min(b.getToAmount()).subtract(b.getFromAmount());
            if (bracketIncome.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal taxInBracket = bracketIncome.multiply(b.getRate())
                    .setScale(0, RoundingMode.HALF_UP);

            PayrollResponse.TaxBracketBreakdown item = new PayrollResponse.TaxBracketBreakdown();
            item.setBracket(b.getBracketOrder());
            item.setFromAmount(b.getFromAmount());
            item.setToAmount(b.getToAmount());
            item.setRate(b.getRate());
            item.setTaxableInBracket(bracketIncome);
            item.setTaxInBracket(taxInBracket);
            breakdown.add(item);
        }
        res.setTaxBrackets(breakdown);
    }
}
