package com.frezo.qlns.service.impl.payroll;

import com.frezo.common.exception.QTHTException;
import com.frezo.qlns.common.AttendanceStatus;
import com.frezo.qlns.common.StatusContarct;
import com.frezo.qlns.engine.PayrollEngine;
import com.frezo.qlns.entity.Contract;
import com.frezo.qlns.entity.LeaveRecord;
import com.frezo.qlns.repository.AttendanceRepository;
import com.frezo.qlns.repository.ContractRepository;
import com.frezo.qlns.repository.EmployeeDependentRepository;
import com.frezo.qlns.repository.LeaveRecordRepository;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Thu thập dữ liệu đầu vào cho 1 lần tính lương: person + contract + chấm công + phép + số người phụ thuộc.
 * <p>
 * Đóng gói thành {@link CollectedInput} để orchestrator chỉ cần 1 lần gọi collect().
 */
@Component
@RequiredArgsConstructor
public class PayrollDataCollector {

    private final PersonRepository personRepository;
    private final ContractRepository contractRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRecordRepository leaveRepository;
    private final EmployeeDependentRepository employeeDependentRepository;

    /** Bundle dữ liệu đã collect — kèm engine input đã build sẵn. */
    public record CollectedInput(
            Person person,
            Contract contract,
            int dependentCount,
            PayrollEngine.PayrollInput engineInput) {}

    /**
     * Load Person, chưa collect. Dùng để service caller lấy {@code orgId} trước khi load config.
     */
    public Person findPersonOrThrow(String personId, java.util.function.Supplier<RuntimeException> errorSupplier) {
        return personRepository.findById(personId).orElseThrow(errorSupplier);
    }

    public Person findPersonOrNull(String personId) {
        if (personId == null || personId.isBlank()) return null;
        return personRepository.findById(personId).orElse(null);
    }

    /** NV active (+ department) — candidates cho calculate-all / daily roster. */
    public List<Person> findActivePersons() {
        return personRepository.findActiveWithDepartment(null, null);
    }

    /**
     * HĐ đủ điều kiện tính lương: {@code activated=true} <b>và</b> {@code status=ACTIVE}
     * (khớp seed/backfill {@code ContractDataInitializer} + QA-BE-PAY-001).
     */
    public boolean isEligibleContract(Contract c) {
        if (c == null) return false;
        return Boolean.TRUE.equals(c.getActivated()) && c.getStatus() == StatusContarct.ACTIVE;
    }

    /**
     * Thu thập toàn bộ input tính lương cho {@code personId} tháng/năm chỉ định.
     * Thiếu HĐ eligible → 400 nghiệp vụ (không fallback lương cứng).
     */
    public CollectedInput collect(Person person, Integer month, Integer year, PayrollConfigLoader.ConfigBundle cfg) {
        String personId = person.getId();

        Contract contract = contractRepository.findAll(
                        (root, query, cb) -> cb.equal(root.get("personId"), personId))
                .stream()
                .filter(this::isEligibleContract)
                .findFirst()
                .orElseThrow(() -> new QTHTException(
                        "Không thể tính lương: nhân viên chưa có hợp đồng đang hiệu lực (activated/ACTIVE). personId="
                                + personId));

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

        BigDecimal basicSalary = contract.getValue() != null
                ? BigDecimal.valueOf(contract.getValue())
                : BigDecimal.ZERO;

        int dependentCount = employeeDependentRepository.findByPersonIdAndIsActiveTrue(personId).size();

        PayrollEngine.PayrollInput input = PayrollEngine.PayrollInput.builder()
                .basicSalary(basicSalary)
                .insuranceSalary(basicSalary)
                .standardDays(cfg.standardDays())
                .standardHoursPerDay(cfg.standardHoursPerDay())
                .workingDays(workingDays)
                .actualWorkingDays(BigDecimal.valueOf(workingDays))
                .leavesPaid(leavePaidCount)
                .leavesUnpaid(leaveUnpaidCount)
                .totalLateMinutes(lateMinutes)
                .overtimeHoursNormal(BigDecimal.valueOf(overtimeMinutes / 60.0)
                        .setScale(1, RoundingMode.HALF_UP))
                .overtimeHoursWeekend(BigDecimal.ZERO)
                .overtimeHoursHoliday(BigDecimal.ZERO)
                .allowance(BigDecimal.ZERO)
                .bonus(BigDecimal.ZERO)
                .advanceDeduction(BigDecimal.ZERO)
                .otherDeductions(BigDecimal.ZERO)
                .dependentCount(dependentCount)
                .payrollConfig(cfg.payrollConfig())
                .insuranceConfig(cfg.insuranceConfig())
                .taxConfigs(cfg.taxConfigs())
                .build();

        return new CollectedInput(person, contract, dependentCount, input);
    }

    /** Duyệt tất cả hợp đồng — dùng cho calculateAll để iterate personId. */
    public List<Contract> findAllContracts() {
        return contractRepository.findAll();
    }

    /**
     * Gom contract theo personId (giữ thứ tự xuất hiện) — calculate-all tính 1 lần / người.
     */
    public Map<String, List<Contract>> groupContractsByPersonId() {
        Map<String, List<Contract>> byPerson = new LinkedHashMap<>();
        for (Contract c : findAllContracts()) {
            if (c.getPersonId() == null) continue;
            byPerson.computeIfAbsent(c.getPersonId(), k -> new java.util.ArrayList<>()).add(c);
        }
        return byPerson;
    }

    public boolean hasEligibleContract(List<Contract> contracts) {
        if (contracts == null || contracts.isEmpty()) return false;
        return contracts.stream().filter(Objects::nonNull).anyMatch(this::isEligibleContract);
    }
}
