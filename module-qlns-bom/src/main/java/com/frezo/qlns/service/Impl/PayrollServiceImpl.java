package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.response.PageResponse;
import com.frezo.qlns.common.AttendanceStatus;
import com.frezo.qlns.dto.request.PayrollFilter;
import com.frezo.qlns.dto.response.PayrollResponse;
import com.frezo.qlns.entity.Contract;
import com.frezo.qlns.entity.LeaveRecord;
import com.frezo.qlns.entity.Payroll;
import com.frezo.qlns.mapper.PayrollMapper;
import com.frezo.qlns.repository.AttendanceRepository;
import com.frezo.qlns.repository.ContractRepository;
import com.frezo.qlns.repository.LeaveRecordRepository;
import com.frezo.qlns.repository.PayrollRepository;
import com.frezo.qlns.service.PayrollService;

import com.frezo.qtht.dto.response.PayrollSettingResponse;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.repository.PersonRepository;
import com.frezo.qtht.service.SettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRecordRepository leaveRepository;
    private final ContractRepository contractRepository;
    private final PayrollMapper payrollMapper;
    private final SettingService settingService;
    private final PersonRepository personRepository;

    @Override
    @Transactional
    public PayrollResponse calculateMonthlyPayroll(String personId, Integer month, Integer year) {
        // Lấy thông tin tổ chức của nhân viên để lấy cấu hình lương
        String orgId = personRepository.findById(personId)
                .map(Person::getOrgId)
                .orElseThrow(() -> new QTHTException("error.person.not.found"));

        PayrollSettingResponse payrollSetting = settingService.getPayrollSetting(orgId);

        Contract contract = contractRepository.findAll((root, query, cb) -> cb.equal(root.get("personId"), personId))
                .stream().findFirst().orElse(null);

        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.with(TemporalAdjusters.lastDayOfMonth());

        // Tổng ngày đi làm
        int workingDays = attendanceRepository.countByPersonIdAndMonthAndYearAndStatusIn(
                personId, month, year, List.of(AttendanceStatus.PRESENT, AttendanceStatus.LATE, AttendanceStatus.HALF_DAY));

        // Phút muộn / Overtime
        int lateMinutes = attendanceRepository.sumLateMinutesByPersonIdAndMonthAndYear(personId, month, year);
        int overtimeMinutes = attendanceRepository.sumOvertimeMinutesByPersonIdAndMonthAndYear(personId, month, year);

        // Ngày nghỉ
        List<LeaveRecord> leaves = leaveRepository.findApprovedByPersonAndMonth(personId, start, end);
        int leaveDays = leaves.size(); 

        BigDecimal basicSalary = (contract != null && contract.getValue() != null) ? BigDecimal.valueOf(contract.getValue()) : BigDecimal.valueOf(10000000);
        
        // Sử dụng cấu hình từ Setting
        int standardDays = payrollSetting.getStandardWorkingDays(); 
        BigDecimal latePenalty = BigDecimal.valueOf(lateMinutes * payrollSetting.getLatePenaltyPerMinute());
        BigDecimal overtimePay = BigDecimal.valueOf(overtimeMinutes * payrollSetting.getOvertimePayPerMinute());

        BigDecimal salaryPerDay = basicSalary.divide(BigDecimal.valueOf(standardDays), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal actualSalary = salaryPerDay.multiply(BigDecimal.valueOf(workingDays));

        Payroll payroll = payrollRepository.findByPersonIdAndMonthAndYear(personId, month, year)
                .orElse(Payroll.builder()
                        .personId(personId)
                        .month(month)
                        .year(year)
                        .build());

        // Update fields
        payroll.setBasicSalary(basicSalary);
        payroll.setStandardDays(standardDays);
        payroll.setWorkingDays(workingDays);
        payroll.setLeavesPaid(0); 
        payroll.setLeavesUnpaid(leaveDays);
        payroll.setTotalLateMinutes(lateMinutes);
        payroll.setLatePenalty(latePenalty);
        payroll.setOvertimePay(overtimePay);
        payroll.setAllowance(BigDecimal.ZERO);
        
        if (payroll.getBonus() == null) payroll.setBonus(BigDecimal.ZERO);
        if (payroll.getDeduction() == null) payroll.setDeduction(BigDecimal.ZERO);

        BigDecimal netSalary = actualSalary
                .add(overtimePay)
                .subtract(latePenalty)
                .add(payroll.getBonus())
                .subtract(payroll.getDeduction());
        
        if(netSalary.compareTo(BigDecimal.ZERO) < 0) {
            netSalary = BigDecimal.ZERO;
        }

        payroll.setNetSalary(netSalary);
        
        if (payroll.getStatus() == null) {
            payroll.setStatus(0); // DRAFT
        }

        Payroll saved = payrollRepository.save(payroll);
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
                // log error but continue
            }
        }
    }

    @Override
    public PayrollResponse updateBonus(String id, Double bonus, Double deduction, String note) {
        Payroll p = payrollRepository.findById(id).orElseThrow(() -> new QTHTException("error.payroll.not.found"));
        if (p.getStatus() != 0) {
            throw new QTHTException("error.payroll.cannot.update"); 
        }
        p.setBonus(BigDecimal.valueOf(bonus));
        p.setDeduction(BigDecimal.valueOf(deduction));
        p.setNote(note);

        BigDecimal salaryPerDay = p.getBasicSalary().divide(BigDecimal.valueOf(p.getStandardDays()), 2, java.math.RoundingMode.HALF_UP);
        BigDecimal actualSalary = salaryPerDay.multiply(BigDecimal.valueOf(p.getWorkingDays()));
        BigDecimal net = actualSalary.add(p.getOvertimePay()).subtract(p.getLatePenalty()).add(p.getBonus()).subtract(p.getDeduction());
        
        p.setNetSalary(net.max(BigDecimal.ZERO));
        
        return payrollMapper.toResponse(payrollRepository.save(p));
    }

    @Override
    public PayrollResponse confirm(String id) {
        Payroll p = payrollRepository.findById(id).orElseThrow(() -> new QTHTException("error.payroll.not.found"));
        p.setStatus(1); 
        return payrollMapper.toResponse(payrollRepository.save(p));
    }

    @Override
    public PayrollResponse pay(String id) {
        Payroll p = payrollRepository.findById(id).orElseThrow(() -> new QTHTException("error.payroll.not.found"));
        p.setStatus(2); 
        p.setPaidAt(LocalDate.now());
        return payrollMapper.toResponse(payrollRepository.save(p));
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
        Page<Payroll> pageResult = payrollRepository.findAll(spec, ServiceHelper.createPageable(filter.getPageNumber(), filter.getPageSize(), sort));
        List<PayrollResponse> items = pageResult.getContent().stream().map(payrollMapper::toResponse).toList();

        return PageResponse.of(filter.getPageNumber(), filter.getPageSize(), pageResult, items);
    }

    @Override
    public PayrollResponse getById(String id) {
        return payrollRepository.findById(id).map(payrollMapper::toResponse)
                .orElseThrow(() -> new QTHTException("error.payroll.not.found"));
    }
}
