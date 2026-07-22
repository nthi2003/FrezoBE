package com.frezo.qlns.service.impl.payroll;

import com.frezo.common.exception.QTHTException;
import com.frezo.qlns.dto.response.PayrollCalculateAllResponse;
import com.frezo.qlns.engine.PayrollEngine;
import com.frezo.qlns.entity.Contract;
import com.frezo.qlns.entity.Payroll;
import com.frezo.qlns.repository.PayrollRepository;
import com.frezo.qtht.entity.Person;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Điều phối flow tính lương end-to-end cho 1 nhân viên hoặc toàn bộ NV trong kỳ.
 * <p>
 * KHÔNG chịu trách nhiệm enrich response — trả về {@link Payroll} entity + {@link CalculationResult}
 * để caller tuỳ ý enrich (tránh dependency ngược lên enricher).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PayrollCalculationOrchestrator {

    public static final String REASON_NO_ACTIVE_CONTRACT = "NO_ACTIVE_CONTRACT";

    private final PayrollRepository payrollRepository;
    private final PayrollEngine payrollEngine;
    private final PayrollDataCollector dataCollector;
    private final PayrollConfigLoader configLoader;
    private final PayrollDetailWriter detailWriter;
    private final PayrollLifecycleService lifecycleService;
    private final PlatformTransactionManager transactionManager;

    /**
     * Bundle trả về sau khi tính — bao gồm Payroll đã lưu và context để enricher dùng lại
     * không phải query lại DB.
     */
    public record CalculationResult(
            Payroll payroll,
            Person person,
            PayrollConfigLoader.ConfigBundle configs,
            int dependentCount) {}

    /**
     * Tính lương cho 1 nhân viên trong 1 kỳ. Idempotent — nếu đã có phiếu (personId+month+year)
     * và chưa lock, sẽ cập nhật lại.
     * <p>
     * Thiếu HĐ activated/ACTIVE → {@link QTHTException} 400 (không fallback lương cứng, không 500).
     */
    @Transactional
    public CalculationResult calculate(String personId, Integer month, Integer year) {
        validatePeriod(month, year);
        return doCalculate(personId, month, year);
    }

    /**
     * Tính lương hàng loạt: 1 TX / person (tránh UnexpectedRollback khi 1 NV lỗi),
     * skip NV thiếu HĐ eligible (không im lặng), trả summary errors cho FE.
     * <p>
     * Không bọc {@code @Transactional} ngoài vòng lặp — catch + commit cùng TX gây 500 cả batch.
     * <p>
     * Candidates = NV active (+ person còn HĐ nhưng chưa trong roster active).
     */
    public PayrollCalculateAllResponse calculateAll(Integer month, Integer year) {
        validatePeriod(month, year);

        Map<String, List<Contract>> contractsByPerson = dataCollector.groupContractsByPersonId();
        Map<String, Person> candidates = buildCandidates(contractsByPerson);

        TransactionTemplate tt = new TransactionTemplate(transactionManager);

        int success = 0;
        int skipped = 0;
        int failed = 0;
        List<PayrollCalculateAllResponse.ItemError> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>(configLoader.missingYearConfigWarnings(year));

        for (Map.Entry<String, Person> entry : candidates.entrySet()) {
            String personId = entry.getKey();
            Person person = entry.getValue();
            List<Contract> contracts = contractsByPerson.getOrDefault(personId, List.of());

            if (!dataCollector.hasEligibleContract(contracts)) {
                skipped++;
                errors.add(itemError(person, personId, REASON_NO_ACTIVE_CONTRACT));
                continue;
            }

            try {
                tt.executeWithoutResult(status -> doCalculate(personId, month, year));
                success++;
            } catch (Exception e) {
                failed++;
                String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                log.error("Error calculating payroll for person {} (month={}, year={}): {}",
                        personId, month, year, reason, e);
                errors.add(itemError(person, personId, reason));
            }
        }

        if (skipped > 0) {
            warnings.add(skipped + " nhân viên bị bỏ qua vì thiếu hợp đồng đang hiệu lực (NO_ACTIVE_CONTRACT).");
        }

        log.info("calculate-all done month={} year={} success={} skipped={} errors={} total={}",
                month, year, success, skipped, failed, candidates.size());

        return PayrollCalculateAllResponse.builder()
                .month(month)
                .year(year)
                .successCount(success)
                .skippedCount(skipped)
                .errorCount(failed)
                .totalCandidates(candidates.size())
                .errors(errors)
                .warnings(warnings)
                .build();
    }

    /**
     * Active persons trước; bổ sung personId chỉ có HĐ (tránh bỏ sót nếu activated=false nhưng còn contract).
     */
    private Map<String, Person> buildCandidates(Map<String, List<Contract>> contractsByPerson) {
        Map<String, Person> candidates = new LinkedHashMap<>();
        for (Person p : dataCollector.findActivePersons()) {
            if (p.getId() != null) {
                candidates.put(p.getId(), p);
            }
        }
        for (String personId : contractsByPerson.keySet()) {
            if (candidates.containsKey(personId)) continue;
            Person p = dataCollector.findPersonOrNull(personId);
            if (p != null) {
                candidates.put(personId, p);
            } else {
                // Giữ slot để skip/error vẫn có personId (không silent drop)
                candidates.put(personId, null);
            }
        }
        return candidates;
    }

    private static PayrollCalculateAllResponse.ItemError itemError(
            Person person, String personId, String reason) {
        return PayrollCalculateAllResponse.ItemError.builder()
                .personId(personId)
                .personName(person != null ? person.getName() : null)
                .personCode(person != null ? person.getCode() : null)
                .reason(reason)
                .build();
    }

    private CalculationResult doCalculate(String personId, Integer month, Integer year) {
        Person person = dataCollector.findPersonOrThrow(personId,
                () -> new QTHTException("error.person.not.found"));

        PayrollConfigLoader.ConfigBundle cfg = configLoader.load(person.getOrgId(), year);
        PayrollDataCollector.CollectedInput input = dataCollector.collect(person, month, year, cfg);

        PayrollEngine.PayrollResult result = payrollEngine.calculate(input.engineInput());

        Payroll payroll = payrollRepository
                .findByPersonIdAndMonthAndYear(personId, month, year)
                .orElse(Payroll.builder()
                        .personId(personId)
                        .month(month)
                        .year(year)
                        .build());

        // Phiếu đã confirm/pay hoặc kỳ khoá → lỗi nghiệp vụ (batch bắt vào errors[], không 500)
        lifecycleService.assertNotLocked(payroll);

        PayrollCalculationHelper.applyResultToEntity(
                payroll, result,
                input.contract() != null ? input.contract().getId() : null);
        if (payroll.getStatus() == null) payroll.setStatus(0);

        Payroll saved = payrollRepository.save(payroll);
        detailWriter.saveAll(saved, result);

        return new CalculationResult(saved, person, cfg, input.dependentCount());
    }

    private static void validatePeriod(Integer month, Integer year) {
        if (month == null || month < 1 || month > 12) {
            throw new QTHTException("Tham số month không hợp lệ (1–12)");
        }
        if (year == null || year < 2000 || year > 2100) {
            throw new QTHTException("Tham số year không hợp lệ");
        }
    }
}
