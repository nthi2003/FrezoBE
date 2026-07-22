package com.frezo.qlns.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.response.PageResponse;
import com.frezo.qlns.dto.request.PayrollFilter;
import com.frezo.qlns.dto.response.PayrollCalculateAllResponse;
import com.frezo.qlns.dto.response.PayrollDetailResponse;
import com.frezo.qlns.dto.response.PayrollResponse;
import com.frezo.qlns.entity.Payroll;
import com.frezo.qlns.repository.PayrollRepository;
import com.frezo.qlns.service.PayrollService;
import com.frezo.qlns.service.impl.payroll.PayrollCalculationOrchestrator;
import com.frezo.qlns.service.impl.payroll.PayrollDetailWriter;
import com.frezo.qlns.service.impl.payroll.PayrollEnricher;
import com.frezo.qlns.service.impl.payroll.PayrollLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Façade cho toàn bộ nghiệp vụ payroll.
 * <p>
 * Sau refactor Batch B (2026-07): class này giảm từ 15 deps → 5 deps bằng cách delegate
 * cho các sub-service theo domain concern:
 * <ul>
 *   <li>{@link PayrollCalculationOrchestrator} — flow tính lương (engine + collector + config + detail)</li>
 *   <li>{@link PayrollLifecycleService} — confirm/pay/delete/updateBonus + lock validation</li>
 *   <li>{@link PayrollEnricher} — bổ sung person/insurance/tax breakdown vào response</li>
 *   <li>{@link PayrollDetailWriter} — quản lý dòng chi tiết phiếu lương</li>
 * </ul>
 * Interface {@link PayrollService} public không đổi để không phá controller.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;
    private final PayrollCalculationOrchestrator calculationOrchestrator;
    private final PayrollLifecycleService lifecycleService;
    private final PayrollEnricher enricher;
    private final PayrollDetailWriter detailWriter;

    @Override
    @Transactional
    public PayrollResponse calculateMonthlyPayroll(String personId, Integer month, Integer year) {
        PayrollCalculationOrchestrator.CalculationResult r =
                calculationOrchestrator.calculate(personId, month, year);
        // Enrich với context vừa tính để tránh query lại DB
        return enricher.enrichWithContext(
                r.payroll(), r.person(),
                r.configs().insuranceConfig(),
                r.configs().taxConfigs(),
                r.dependentCount());
    }

    @Override
    public PayrollCalculateAllResponse calculateAllPayroll(Integer month, Integer year) {
        // Không @Transactional ngoài — orchestrator dùng 1 TX / person + summary skip/error.
        return calculationOrchestrator.calculateAll(month, year);
    }

    @Override
    @Transactional
    public PayrollResponse updateBonus(String id, Double bonus, Double deduction, String note) {
        Payroll saved = lifecycleService.updateBonus(id, bonus, deduction, note);
        return enricher.enrichFull(saved);
    }

    @Override
    @Transactional
    public PayrollResponse confirm(String id) {
        return enricher.enrichFull(lifecycleService.confirm(id));
    }

    @Override
    @Transactional
    public PayrollResponse pay(String id) {
        return enricher.enrichFull(lifecycleService.pay(id));
    }

    @Override
    public void deletePayroll(String id) {
        lifecycleService.softDelete(id);
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
        // Default pageNumber=1 khi omit (1-based như ServiceHelper) — tránh NPE unbox null → PageResponse.of(int,…)
        int pageNum = filter.getPageNumber() != null ? filter.getPageNumber() : 1;
        int pageSize = filter.getPageSize() != null ? filter.getPageSize() : 10;
        Page<Payroll> pageResult = payrollRepository.findAll(spec,
                ServiceHelper.createPageable(pageNum, pageSize, sort));

        // Dùng light enrich cho list view — tránh N+1 tax breakdown
        List<PayrollResponse> items = pageResult.getContent().stream()
                .map(enricher::enrichLight)
                .toList();
        return PageResponse.of(pageNum, pageSize, pageResult, items);
    }

    @Override
    public PayrollResponse getById(String id) {
        return payrollRepository.findById(id)
                .map(enricher::enrichFull)
                .orElseThrow(() -> new QTHTException("error.payroll.not.found"));
    }

    @Override
    public List<PayrollDetailResponse> getPayrollDetails(String payrollId) {
        return detailWriter.getDetails(payrollId);
    }
}
