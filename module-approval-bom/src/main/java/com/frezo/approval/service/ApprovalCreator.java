package com.frezo.approval.service;

import com.frezo.approval.entity.ApprovalFlow;
import com.frezo.approval.entity.ApprovalFlowStep;
import com.frezo.approval.entity.ApprovalRequest;
import com.frezo.approval.entity.ApprovalStep;
import com.frezo.approval.repository.ApprovalFlowRepository;
import com.frezo.approval.repository.ApprovalFlowStepRepository;
import com.frezo.approval.repository.ApprovalRequestRepository;
import com.frezo.approval.repository.ApprovalStepRepository;
import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.helper.SystemUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Tạo ApprovalRequest + các ApprovalStep từ flow template.
 */
@Component
@RequiredArgsConstructor
public class ApprovalCreator {

    private final ApprovalFlowRepository flowRepository;
    private final ApprovalFlowStepRepository flowStepRepository;
    private final ApprovalRequestRepository requestRepository;
    private final ApprovalStepRepository stepRepository;
    private final ApproverResolver approverResolver;

    @Transactional
    public ApprovalRequest create(String subjectType, String subjectId, String subjectSummary,
                                  String flowId, String requestedBy) {
        return create(subjectType, subjectId, subjectSummary, flowId, null, requestedBy);
    }

    @Transactional
    public ApprovalRequest create(String subjectType, String subjectId, String subjectSummary,
                                  String flowId, String flowCode, String requestedBy) {
        ApprovalFlow flow = resolveFlow(subjectType, flowId, flowCode);
        List<ApprovalFlowStep> templates = flowStepRepository
                .findByFlowIdAndIsDeletedFalseOrderByStepOrderAsc(flow.getId());
        if (templates.isEmpty()) {
            throw new AppException(CommonErrorCode.INVALID_REQUEST, "Flow chưa có bước duyệt");
        }

        String by = requestedBy != null ? requestedBy : SystemUtils.getCurrentUsername();
        ApprovalFlowStep first = templates.get(0);

        ApprovalRequest req = ApprovalRequest.builder()
                .flowId(flow.getId())
                .subjectType(subjectType)
                .subjectId(subjectId)
                .subjectSummary(subjectSummary)
                .requestedBy(by)
                .requestedAt(LocalDateTime.now())
                .currentStepOrder(first.getStepOrder())
                .totalSteps(templates.size())
                .status("PENDING")
                .currentApproverHint(first.getApproverRole())
                .build();
        req.setId(UUID.randomUUID().toString());
        req = requestRepository.save(req);

        ApprovalStep submitted = ApprovalStep.builder()
                .requestId(req.getId())
                .stepOrder(0)
                .approverUsername(by)
                .approverName(approverResolver.displayName(by))
                .action("SUBMITTED")
                .actionedAt(LocalDateTime.now())
                .build();
        submitted.setId(UUID.randomUUID().toString());
        stepRepository.save(submitted);

        for (ApprovalFlowStep t : templates) {
            ApproverResolver.ApproverHint h = approverResolver.resolveFirst(t.getApproverRole()).orElse(null);
            ApprovalStep step = ApprovalStep.builder()
                    .requestId(req.getId())
                    .stepOrder(t.getStepOrder())
                    .approverRole(t.getApproverRole())
                    .approverPersonId(h != null ? h.personId() : null)
                    .approverUsername(h != null ? h.username() : null)
                    .approverName(h != null ? h.displayName() : t.getName())
                    .action("PENDING")
                    .build();
            step.setId(UUID.randomUUID().toString());
            stepRepository.save(step);
        }
        return req;
    }

    private ApprovalFlow resolveFlow(String subjectType, String flowId, String flowCode) {
        if (flowId != null && !flowId.isBlank()) {
            return flowRepository.findById(flowId)
                    .filter(f -> Boolean.FALSE.equals(f.getIsDeleted()))
                    .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Flow không tồn tại"));
        }
        if (flowCode != null && !flowCode.isBlank()) {
            return flowRepository.findByCodeAndActiveTrueAndIsDeletedFalse(flowCode)
                    .or(() -> flowRepository.findByCodeAndIsDeletedFalse(flowCode))
                    .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND,
                            "Không có flow code " + flowCode));
        }
        return flowRepository.findFirstBySubjectTypeAndActiveTrueAndIsDeletedFalse(subjectType)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND,
                        "Không có flow active cho " + subjectType));
    }
}
