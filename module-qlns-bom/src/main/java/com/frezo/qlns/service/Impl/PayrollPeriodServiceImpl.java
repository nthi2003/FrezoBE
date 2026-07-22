package com.frezo.qlns.service.impl;

import com.frezo.approval.entity.ApprovalRequest;
import com.frezo.approval.service.ApprovalCreator;
import com.frezo.common.domain.SubjectType;
import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.response.PageResponse;
import com.frezo.common.workflow.dto.WorkflowInstanceDto;
import com.frezo.common.workflow.dto.WorkflowTaskDto;
import com.frezo.common.workflow.service.WorkflowService;
import com.frezo.qlns.dto.request.PayrollPeriodRequest;
import com.frezo.qlns.dto.response.PayrollPeriodResponse;
import com.frezo.qlns.entity.PayrollPeriod;
import com.frezo.qlns.mapper.PayrollPeriodMapper;
import com.frezo.qlns.repository.PayrollPeriodRepository;
import com.frezo.qlns.service.PayrollPeriodService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Quản lý kỳ lương — CRUD + wire vào Workflow Engine.
 *
 * <h3>State machine (cũ, giữ backward-compat)</h3>
 * <pre>
 *   0 (Mở) ──(lock)──▶ 1 (Đã khoá / Đang duyệt) ──(close)──▶ 2 (Đã đóng)
 *      ▲                    │
 *      └──(unlock)──────────┘
 * </pre>
 *
 * <h3>State machine với Workflow Engine</h3>
 * <pre>
 *   0 (Mở) ──(lock)──▶ 1 + START workflow instance (task PENDING theo definition)
 *                            │
 *                            ├─ approve các bước → khi COMPLETED → 2 (Đã đóng, chi lương)
 *                            └─ reject → về 0 (Mở lại)
 * </pre>
 *
 * Admin cấu hình flow ở {@code /qtht/workflows} với definition {@code PAYROLL_DEFAULT}:
 * mặc định 1 bước "Kế toán duyệt" — có thể thêm "Giám đốc ký" / "Thanh tra lương".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PayrollPeriodServiceImpl implements PayrollPeriodService {

    // ---- Status constants ----
    public static final int STATUS_OPEN = 0;
    public static final int STATUS_LOCKED = 1;
    public static final int STATUS_CLOSED = 2;

    // ---- Workflow engine binding ----
    public static final String WF_ENTITY_TYPE = "PAYROLL_PERIOD";
    public static final String WF_DEF_CODE = "PAYROLL_DEFAULT";
    public static final String APPROVAL_FLOW_CODE = "PAYROLL_PERIOD";

    private final PayrollPeriodRepository payrollPeriodRepository;
    private final PayrollPeriodMapper payrollPeriodMapper;
    private final ApprovalCreator approvalCreator;
    /** Legacy workflow — giữ cho unlock/approve cũ nếu còn instance. */
    private final WorkflowService workflowService;

    // ============================================================
    // CRUD
    // ============================================================

    @Override
    @Transactional
    public PayrollPeriodResponse create(PayrollPeriodRequest request) {
        if (payrollPeriodRepository.findByOrgIdAndMonthAndYear(request.getOrgId(), request.getMonth(), request.getYear()).isPresent()) {
            throw new QTHTException("Kỳ lương đã tồn tại cho tháng " + request.getMonth() + "/" + request.getYear());
        }
        PayrollPeriod entity = PayrollPeriod.builder()
                .orgId(request.getOrgId())
                .month(request.getMonth())
                .year(request.getYear())
                .name(request.getName())
                .status(STATUS_OPEN)
                .fromDate(request.getFromDate())
                .toDate(request.getToDate())
                .paymentDate(request.getPaymentDate())
                .note(request.getNote())
                .build();
        return enrich(payrollPeriodRepository.save(entity));
    }

    @Override
    @Transactional
    public PayrollPeriodResponse update(String id, PayrollPeriodRequest request) {
        PayrollPeriod entity = findOrThrow(id);
        if (entity.getStatus() != null && (entity.getStatus() == STATUS_LOCKED || entity.getStatus() == STATUS_CLOSED)) {
            throw new QTHTException("Không thể sửa kỳ lương đã khóa");
        }
        entity.setName(request.getName());
        entity.setFromDate(request.getFromDate());
        entity.setToDate(request.getToDate());
        entity.setPaymentDate(request.getPaymentDate());
        entity.setNote(request.getNote());
        return enrich(payrollPeriodRepository.save(entity));
    }

    // ============================================================
    // Workflow-aware transitions
    // ============================================================

    @Override
    @Transactional
    public PayrollPeriodResponse lock(String id) {
        PayrollPeriod entity = findOrThrow(id);
        if (entity.getStatus() != null && (entity.getStatus() == STATUS_LOCKED || entity.getStatus() == STATUS_CLOSED)) {
            throw new QTHTException("Kỳ lương đã được khóa");
        }
        entity.setStatus(STATUS_LOCKED);
        entity.setLockedAt(LocalDateTime.now());
        entity.setLockedBy(SystemUtils.getCurrentUsername());

        String title = String.format("Kỳ lương %02d/%d%s",
                entity.getMonth(), entity.getYear(),
                entity.getName() != null ? " · " + entity.getName() : "");
        ApprovalRequest req = approvalCreator.create(
                SubjectType.PAYROLL.name(),
                entity.getId(),
                title,
                null,
                APPROVAL_FLOW_CODE,
                null);
        entity.setApprovalRequestId(req.getId());
        log.info("[payroll] Khoá kỳ {} + Approval {}", entity.getId(), req.getId());
        return enrich(payrollPeriodRepository.save(entity));
    }

    @Override
    @Transactional
    public PayrollPeriodResponse unlock(String id) {
        PayrollPeriod entity = findOrThrow(id);
        if (entity.getStatus() != null && entity.getStatus() == STATUS_CLOSED) {
            throw new QTHTException("Không thể mở khóa kỳ lương đã đóng");
        }

        // Cancel workflow instance nếu đang chạy — để không kẹt task PENDING orphan
        if (entity.getWorkflowInstanceId() != null) {
            try {
                workflowService.cancelInstance(entity.getWorkflowInstanceId());
            } catch (Exception ex) {
                log.warn("[payroll] Không cancel được workflow {}: {}",
                        entity.getWorkflowInstanceId(), ex.getMessage());
            }
            entity.setWorkflowInstanceId(null);
        }
        entity.setStatus(STATUS_OPEN);
        entity.setLockedAt(null);
        entity.setLockedBy(null);
        return enrich(payrollPeriodRepository.save(entity));
    }

    @Override
    @Transactional
    public PayrollPeriodResponse close(String id) {
        PayrollPeriod entity = findOrThrow(id);
        entity.setStatus(STATUS_CLOSED);
        return enrich(payrollPeriodRepository.save(entity));
    }

    @Override
    @Transactional
    public PayrollPeriodResponse approve(String id, String note) {
        PayrollPeriod entity = findOrThrow(id);
        if (entity.getWorkflowInstanceId() == null) {
            throw new QTHTException("Kỳ lương chưa có workflow — bấm 'Khoá kỳ' trước để bắt đầu duyệt.");
        }
        WorkflowTaskDto task = findCurrentPendingTask(entity);
        if (task == null) {
            // Có thể instance đã done trước đó nhưng entity chưa sync — force sync
            syncFromEngine(entity);
            return enrich(payrollPeriodRepository.save(entity));
        }
        workflowService.approveTask(task.getId(), note);

        // Sau khi approve, check instance status
        WorkflowInstanceDto inst = workflowService
                .findInstanceByEntity(WF_ENTITY_TYPE, entity.getId())
                .orElse(null);
        if (inst != null && "COMPLETED".equals(inst.getStatus())) {
            entity.setStatus(STATUS_CLOSED);
            log.info("[payroll] Kỳ {} duyệt xong toàn bộ workflow → CLOSED", entity.getId());
        }
        return enrich(payrollPeriodRepository.save(entity));
    }

    @Override
    @Transactional
    public PayrollPeriodResponse reject(String id, String reason) {
        PayrollPeriod entity = findOrThrow(id);
        if (reason == null || reason.isBlank()) {
            throw new QTHTException("Vui lòng nhập lý do từ chối");
        }
        if (entity.getWorkflowInstanceId() == null) {
            throw new QTHTException("Kỳ lương chưa có workflow");
        }
        WorkflowTaskDto task = findCurrentPendingTask(entity);
        if (task == null) {
            throw new QTHTException("Không có bước nào đang chờ duyệt");
        }
        workflowService.rejectTask(task.getId(), reason.trim());

        // Sau reject, mở khoá kỳ lương để accountant chỉnh sửa lại
        entity.setStatus(STATUS_OPEN);
        entity.setLockedAt(null);
        entity.setLockedBy(null);
        entity.setWorkflowInstanceId(null);
        return enrich(payrollPeriodRepository.save(entity));
    }

    // ============================================================
    // Query
    // ============================================================

    @Override
    public PayrollPeriodResponse getById(String id) {
        PayrollPeriod entity = findOrThrow(id);
        // Force sync để FE luôn thấy state mới nhất kể cả có race giữa các UI
        syncFromEngine(entity);
        return enrich(entity);
    }

    @Override
    public PageResponse<PayrollPeriodResponse> getAll(String orgId, Integer month, Integer year, Integer status,
                                                       Integer pageNumber, Integer pageSize) {
        Specification<PayrollPeriod> spec = Specification.where(GenericSpecification.equalField("isDeleted", false));
        if (orgId != null) spec = spec.and(GenericSpecification.equalField("orgId", orgId));
        if (month != null) spec = spec.and(GenericSpecification.equalField("month", month));
        if (year != null) spec = spec.and(GenericSpecification.equalField("year", year));
        if (status != null) spec = spec.and(GenericSpecification.equalField("status", status));

        Sort sort = Sort.by(Sort.Direction.DESC, "year").and(Sort.by(Sort.Direction.DESC, "month"));
        Page<PayrollPeriod> pageResult = payrollPeriodRepository.findAll(spec,
                ServiceHelper.createPageable(pageNumber, pageSize, sort));
        var items = pageResult.getContent().stream().map(this::enrich).toList();
        return PageResponse.of(pageNumber, pageSize, pageResult, items);
    }

    // ============================================================
    // Internal helpers
    // ============================================================

    private PayrollPeriod findOrThrow(String id) {
        return payrollPeriodRepository.findById(id)
                .orElseThrow(() -> new QTHTException("Không tìm thấy kỳ lương"));
    }

    private WorkflowTaskDto findCurrentPendingTask(PayrollPeriod entity) {
        WorkflowInstanceDto inst = workflowService
                .findInstanceByEntity(WF_ENTITY_TYPE, entity.getId())
                .orElse(null);
        if (inst == null || inst.getTasks() == null) return null;
        return inst.getTasks().stream()
                .filter(t -> "PENDING".equals(t.getStatus()))
                .findFirst().orElse(null);
    }

    /**
     * Đồng bộ status kỳ lương với state của workflow instance — phòng khi có mismatch
     * (VD engine COMPLETED do 1 flow khác trigger, nhưng entity chưa cập nhật).
     */
    private void syncFromEngine(PayrollPeriod entity) {
        if (entity.getWorkflowInstanceId() == null) return;
        try {
            WorkflowInstanceDto inst = workflowService
                    .findInstanceByEntity(WF_ENTITY_TYPE, entity.getId())
                    .orElse(null);
            if (inst == null) return;
            String s = inst.getStatus();
            if ("COMPLETED".equals(s) && entity.getStatus() != STATUS_CLOSED) {
                entity.setStatus(STATUS_CLOSED);
            } else if (("REJECTED".equals(s) || "CANCELLED".equals(s)) && entity.getStatus() == STATUS_LOCKED) {
                entity.setStatus(STATUS_OPEN);
                entity.setLockedAt(null);
                entity.setLockedBy(null);
                entity.setWorkflowInstanceId(null);
            }
        } catch (Exception ex) {
            log.debug("[payroll] syncFromEngine failed for {}: {}", entity.getId(), ex.getMessage());
        }
    }

    /**
     * Enrich response với workflow state (currentTaskId + stepName) để FE render
     * nút "Duyệt: {stepName}" dynamic và stepper đầy đủ.
     */
    private PayrollPeriodResponse enrich(PayrollPeriod entity) {
        PayrollPeriodResponse dto = payrollPeriodMapper.toResponse(entity);
        dto.setApprovalRequestId(entity.getApprovalRequestId());
        if (entity.getWorkflowInstanceId() != null) {
            dto.setWorkflowEntityType(WF_ENTITY_TYPE);
            try {
                WorkflowInstanceDto inst = workflowService
                        .findInstanceByEntity(WF_ENTITY_TYPE, entity.getId())
                        .orElse(null);
                if (inst != null && inst.getTasks() != null) {
                    inst.getTasks().stream()
                            .filter(t -> "PENDING".equals(t.getStatus()))
                            .findFirst()
                            .ifPresent(t -> {
                                dto.setCurrentTaskId(t.getId());
                                dto.setCurrentStepName(t.getStepName());
                            });
                }
            } catch (Exception ex) {
                log.debug("[payroll] Enrich workflow failed for {}: {}", entity.getId(), ex.getMessage());
            }
        }
        return dto;
    }
}
