package com.frezo.qlns.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.response.PageResponse;
import com.frezo.qlns.common.AttendanceStatus;
import com.frezo.qlns.dto.request.PayrollFilter;
import com.frezo.qlns.dto.response.PayrollDetailResponse;
import com.frezo.qlns.dto.response.PayrollResponse;
import com.frezo.qlns.engine.PayrollEngine;
import com.frezo.qlns.entity.*;
import com.frezo.qlns.mapper.PayrollDetailMapper;
import com.frezo.qlns.mapper.PayrollMapper;
import com.frezo.qlns.repository.*;
import com.frezo.qlns.service.PayrollService;
import com.frezo.qtht.dto.response.PayrollSettingResponse;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.repository.PersonRepository;
import com.frezo.qtht.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;
    private final PayrollDetailRepository payrollDetailRepository;
    private final PayrollPeriodRepository payrollPeriodRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRecordRepository leaveRepository;
    private final ContractRepository contractRepository;
    private final PayrollConfigRepository payrollConfigRepository;
    private final InsuranceConfigRepository insuranceConfigRepository;
    private final TaxConfigRepository taxConfigRepository;
    private final EmployeeDependentRepository employeeDependentRepository;
    private final PayrollMapper payrollMapper;
    private final PayrollDetailMapper payrollDetailMapper;
    private final PayrollEngine payrollEngine;
    private final SettingService settingService;
    private final PersonRepository personRepository;

    @Override
    @Transactional
    public PayrollResponse calculateMonthlyPayroll(String personId, Integer month, Integer year) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new QTHTException("error.person.not.found"));
        String orgId = person.getOrgId();

        PayrollSettingResponse payrollSetting = settingService.getPayrollSetting(orgId);
        PayrollConfig payrollConfig = payrollConfigRepository.findByOrgIdAndYearAndIsActiveTrue(orgId, year).orElse(null);
        InsuranceConfig insuranceConfig = insuranceConfigRepository.findByYearAndIsActiveTrue(year).orElse(null);
        List<TaxConfig> taxConfigs = taxConfigRepository.findByYearAndIsActiveTrueOrderByBracketOrderAsc(year);

        Contract contract = contractRepository.findAll(
                        (root, query, cb) -> cb.equal(root.get("personId"), personId))
                .stream().filter(c -> Boolean.TRUE.equals(c.getActivated()))
                .findFirst().orElse(null);

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.with(TemporalAdjusters.lastDayOfMonth());

        int workingDays = attendanceRepository.countByPersonIdAndMonthAndYearAndStatusIn(
                personId, month, year,
                List.of(AttendanceStatus.PRESENT, AttendanceStatus.LATE, AttendanceStatus.HALF_DAY));

        int lateMinutes = attendanceRepository.sumLateMinutesByPersonIdAndMonthAndYear(personId, month, year);
        int overtimeMinutes = attendanceRepository.sumOvertimeMinutesByPersonIdAndMonthAndYear(personId, month, year);

        List<LeaveRecord> leaves = leaveRepository.findApprovedByPersonAndMonth(personId, start, end);
        int leavePaidCount = 0;
        int leaveUnpaidCount = 0;
        for (LeaveRecord l : leaves) {
            if ("UNPAID".equalsIgnoreCase(l.getLeaveType())) {
                leaveUnpaidCount++;
            } else {
                leavePaidCount++;
            }
        }

        BigDecimal basicSalary = (contract != null && contract.getValue() != null)
                ? BigDecimal.valueOf(contract.getValue()) : BigDecimal.valueOf(10000000);
        BigDecimal insuranceSalary = basicSalary;

        int standardDays = payrollSetting.getStandardWorkingDays() != null
                ? payrollSetting.getStandardWorkingDays() : 26;
        int standardHoursPerDay = (payrollConfig != null && payrollConfig.getStandardHoursPerDay() != null)
                ? payrollConfig.getStandardHoursPerDay() : 8;

        BigDecimal salaryPerDay = basicSalary.divide(BigDecimal.valueOf(standardDays), 10, RoundingMode.HALF_UP);
        BigDecimal actualWorkingDays = BigDecimal.valueOf(workingDays);

        int dependentCount = employeeDependentRepository.findByPersonIdAndIsActiveTrue(personId).size();

        PayrollEngine.PayrollInput input = PayrollEngine.PayrollInput.builder()
                .basicSalary(basicSalary)
                .insuranceSalary(insuranceSalary)
                .standardDays(standardDays)
                .standardHoursPerDay(standardHoursPerDay)
                .workingDays(workingDays)
                .actualWorkingDays(actualWorkingDays)
                .leavesPaid(leavePaidCount)
                .leavesUnpaid(leaveUnpaidCount)
                .totalLateMinutes(lateMinutes)
                .overtimeHoursNormal(BigDecimal.valueOf(overtimeMinutes / 60.0).setScale(1, RoundingMode.HALF_UP))
                .overtimeHoursWeekend(BigDecimal.ZERO)
                .overtimeHoursHoliday(BigDecimal.ZERO)
                .allowance(BigDecimal.ZERO)
                .bonus(BigDecimal.ZERO)
                .advanceDeduction(BigDecimal.ZERO)
                .otherDeductions(BigDecimal.ZERO)
                .dependentCount(dependentCount)
                .payrollConfig(payrollConfig)
                .insuranceConfig(insuranceConfig)
                .taxConfigs(taxConfigs)
                .build();

        PayrollEngine.PayrollResult result = payrollEngine.calculate(input);

        Payroll payroll = payrollRepository.findByPersonIdAndMonthAndYear(personId, month, year)
                .orElse(Payroll.builder()
                        .personId(personId)
                        .month(month)
                        .year(year)
                        .build());

        validatePayrollNotLocked(payroll);

        updatePayrollFromResult(payroll, result, contract != null ? contract.getId() : null);
        if (payroll.getStatus() == null) payroll.setStatus(0);

        Payroll saved = payrollRepository.save(payroll);

        savePayrollDetails(saved, result);

        return payrollMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void calculateAllPayroll(Integer month, Integer year) {
        List<Contract> activeContracts = contractRepository.findAll();
        for (Contract c : activeContracts) {
            try {
                if (c.getPersonId() != null) {
                    calculateMonthlyPayroll(c.getPersonId(), month, year);
                }
            } catch (Exception e) {
                log.error("Error calculating payroll for contract {}: {}", c.getId(), e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public PayrollResponse updateBonus(String id, Double bonus, Double deduction, String note) {
        Payroll p = payrollRepository.findById(id)
                .orElseThrow(() -> new QTHTException("error.payroll.not.found"));
        validatePayrollNotLocked(p);

        p.setBonus(BigDecimal.valueOf(bonus));
        p.setOtherDeductions(BigDecimal.valueOf(deduction));
        p.setNote(note);

        recalculatePayroll(p);

        return payrollMapper.toResponse(payrollRepository.save(p));
    }

    @Override
    @Transactional
    public PayrollResponse confirm(String id) {
        Payroll p = payrollRepository.findById(id)
                .orElseThrow(() -> new QTHTException("error.payroll.not.found"));
        if (p.getStatus() != 0) throw new QTHTException("error.payroll.cannot.update");
        p.setStatus(1);
        return payrollMapper.toResponse(payrollRepository.save(p));
    }

    @Override
    @Transactional
    public PayrollResponse pay(String id) {
        Payroll p = payrollRepository.findById(id)
                .orElseThrow(() -> new QTHTException("error.payroll.not.found"));
        if (p.getStatus() != 1) throw new QTHTException("Chỉ có thể thanh toán bảng lương đã xác nhận");
        p.setStatus(2);
        p.setPaidAt(LocalDate.now());
        return payrollMapper.toResponse(payrollRepository.save(p));
    }

    @Override
    public void deletePayroll(String id) {
        Payroll p = payrollRepository.findById(id)
                .orElseThrow(() -> new QTHTException("error.payroll.not.found"));
        validatePayrollNotLocked(p);
        p.setIsDeleted(true);
        payrollRepository.save(p);
    }

    @Override
    public PageResponse<PayrollResponse> getAll(PayrollFilter filter) {
        Specification<Payroll> spec = Specification.where(GenericSpecification.equalField("isDeleted", false));
        if (filter.getContractId() != null) spec = spec.and(GenericSpecification.equalField("contractId", filter.getContractId()));
        if (filter.getPersonId() != null) spec = spec.and(GenericSpecification.equalField("personId", filter.getPersonId()));
        if (filter.getMonth() != null) spec = spec.and(GenericSpecification.equalField("month", filter.getMonth()));
        if (filter.getYear() != null) spec = spec.and(GenericSpecification.equalField("year", filter.getYear()));
        if (filter.getStatus() != null) spec = spec.and(GenericSpecification.equalField("status", filter.getStatus()));

        Sort sort = Sort.by(Sort.Direction.DESC, "year").and(Sort.by(Sort.Direction.DESC, "month"));
        Page<Payroll> pageResult = payrollRepository.findAll(spec,
                ServiceHelper.createPageable(filter.getPageNumber(), filter.getPageSize(), sort));
        List<PayrollResponse> items = pageResult.getContent().stream().map(payrollMapper::toResponse).toList();
        return PageResponse.of(filter.getPageNumber(), filter.getPageSize(), pageResult, items);
    }

    @Override
    public PayrollResponse getById(String id) {
        return payrollRepository.findById(id)
                .map(payrollMapper::toResponse)
                .orElseThrow(() -> new QTHTException("error.payroll.not.found"));
    }

    @Override
    public List<PayrollDetailResponse> getPayrollDetails(String payrollId) {
        return payrollDetailRepository.findByPayrollIdOrderBySortOrderAsc(payrollId)
                .stream().map(payrollDetailMapper::toResponse).toList();
    }

    private void validatePayrollNotLocked(Payroll payroll) {
        if (payroll.getPayrollPeriodId() != null) {
            payrollPeriodRepository.findById(payroll.getPayrollPeriodId()).ifPresent(period -> {
                if (period.getStatus() == 1 || period.getStatus() == 2) {
                    throw new QTHTException("Không thể thay đổi bảng lương trong kỳ đã khóa");
                }
            });
        }
        if (payroll.getStatus() != null && payroll.getStatus() > 0) {
            throw new QTHTException("error.payroll.cannot.update");
        }
    }

    private void updatePayrollFromResult(Payroll payroll, PayrollEngine.PayrollResult result, String contractId) {
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

    private void recalculatePayroll(Payroll p) {
        BigDecimal salaryPerDay = p.getBasicSalary()
                .divide(BigDecimal.valueOf(p.getStandardDays()), 10, RoundingMode.HALF_UP);
        BigDecimal actualSalary = salaryPerDay.multiply(
                p.getActualWorkingDays() != null ? p.getActualWorkingDays() : BigDecimal.valueOf(p.getWorkingDays()));
        if (p.getGrossSalary() == null) {
            p.setGrossSalary(actualSalary.add(p.getOvertimePay() != null ? p.getOvertimePay() : BigDecimal.ZERO)
                    .subtract(p.getLatePenalty() != null ? p.getLatePenalty() : BigDecimal.ZERO)
                    .add(p.getAllowance() != null ? p.getAllowance() : BigDecimal.ZERO)
                    .add(p.getBonus() != null ? p.getBonus() : BigDecimal.ZERO));
        } else {
            p.setGrossSalary(p.getGrossSalary()
                    .subtract(p.getAllowance() != null ? p.getAllowance() : BigDecimal.ZERO)
                    .subtract(p.getBonus() != null ? p.getBonus() : BigDecimal.ZERO)
                    .add(p.getAllowance() != null ? p.getAllowance() : BigDecimal.ZERO)
                    .add(p.getBonus() != null ? p.getBonus() : BigDecimal.ZERO));
        }

        BigDecimal totalDed = BigDecimal.ZERO;
        totalDed = totalDed.add(p.getTotalInsurance() != null ? p.getTotalInsurance() : BigDecimal.ZERO);
        totalDed = totalDed.add(p.getTaxIncome() != null ? p.getTaxIncome() : BigDecimal.ZERO);
        totalDed = totalDed.add(p.getUnionFee() != null ? p.getUnionFee() : BigDecimal.ZERO);
        totalDed = totalDed.add(p.getAdvanceDeduction() != null ? p.getAdvanceDeduction() : BigDecimal.ZERO);
        totalDed = totalDed.add(p.getOtherDeductions() != null ? p.getOtherDeductions() : BigDecimal.ZERO);
        p.setTotalDeductions(totalDed);
        p.setNetSalary(p.getGrossSalary().subtract(totalDed).max(BigDecimal.ZERO));
    }

    private void savePayrollDetails(Payroll payroll, PayrollEngine.PayrollResult result) {
        payrollDetailRepository.deleteByPayrollId(payroll.getId());

        List<PayrollDetail> details = new ArrayList<>();
        int order = 1;

        details.add(buildDetail(payroll.getId(), "BASIC_SALARY", "Lương cơ bản", "EARNING",
                result.getBasicSalary(), order++));
        details.add(buildDetail(payroll.getId(), "ACTUAL_SALARY", "Lương theo công thực tế", "EARNING",
                result.getActualSalary(), order++));
        details.add(buildDetail(payroll.getId(), "OT_NORMAL", "Lương OT ngày thường", "EARNING",
                result.getOvertimePay(), order++));
        details.add(buildDetail(payroll.getId(), "ALLOWANCE", "Phụ cấp", "EARNING",
                result.getAllowance(), order++));
        details.add(buildDetail(payroll.getId(), "BONUS", "Thưởng", "EARNING",
                result.getBonus(), order++));
        details.add(buildDetail(payroll.getId(), "LATE_PENALTY", "Phạt đi muộn", "DEDUCTION",
                result.getLatePenalty(), order++));
        details.add(buildDetail(payroll.getId(), "SOCIAL_INSURANCE", "BHXH", "DEDUCTION",
                result.getSocialInsurance(), order++));
        details.add(buildDetail(payroll.getId(), "HEALTH_INSURANCE", "BHYT", "DEDUCTION",
                result.getHealthInsurance(), order++));
        details.add(buildDetail(payroll.getId(), "UNEMPLOYMENT_INSURANCE", "BHTN", "DEDUCTION",
                result.getUnemploymentInsurance(), order++));
        details.add(buildDetail(payroll.getId(), "UNION_FEE", "Công đoàn phí", "DEDUCTION",
                result.getUnionFee(), order++));
        details.add(buildDetail(payroll.getId(), "TAX_INCOME", "Thuế TNCN", "DEDUCTION",
                result.getTaxIncome(), order++));
        details.add(buildDetail(payroll.getId(), "ADVANCE", "Tạm ứng", "DEDUCTION",
                result.getAdvanceDeduction(), order++));
        details.add(buildDetail(payroll.getId(), "OTHER_DEDUCTION", "Khấu trừ khác", "DEDUCTION",
                result.getOtherDeductions(), order++));
        details.add(buildDetail(payroll.getId(), "NET_SALARY", "Thực lĩnh", "NET",
                result.getNetSalary(), order++));

        payrollDetailRepository.saveAll(details);
    }

    private PayrollDetail buildDetail(String payrollId, String code, String name, String type,
                                       BigDecimal amount, int order) {
        return PayrollDetail.builder()
                .payrollId(payrollId)
                .componentCode(code)
                .componentName(name)
                .componentType(type)
                .amount(amount != null ? amount : BigDecimal.ZERO)
                .sortOrder(order)
                .build();
    }
}
