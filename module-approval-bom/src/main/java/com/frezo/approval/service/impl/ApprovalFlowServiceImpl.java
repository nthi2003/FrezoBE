package com.frezo.approval.service.impl;

import com.frezo.approval.dto.ApprovalFlowDto;
import com.frezo.approval.dto.ApprovalFlowRequest;
import com.frezo.approval.dto.ApprovalFlowStepTemplate;
import com.frezo.approval.entity.ApprovalFlow;
import com.frezo.approval.entity.ApprovalFlowStep;
import com.frezo.approval.repository.ApprovalFlowRepository;
import com.frezo.approval.repository.ApprovalFlowStepRepository;
import com.frezo.approval.service.ApprovalFlowService;
import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApprovalFlowServiceImpl implements ApprovalFlowService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ApprovalFlowRepository flowRepository;
    private final ApprovalFlowStepRepository flowStepRepository;

    @Override
    public List<ApprovalFlowDto> list() {
        return flowRepository.findByIsDeletedFalseOrderByCreatedDateDesc().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    public ApprovalFlowDto create(ApprovalFlowRequest req) {
        ApprovalFlow flow = ApprovalFlow.builder()
                .code("FLOW-" + System.currentTimeMillis())
                .name(req.getName())
                .subjectType(req.getSubjectType())
                .active(req.getActive() == null || req.getActive())
                .description(req.getDescription())
                .build();
        flow.setId(UUID.randomUUID().toString());
        flow = flowRepository.save(flow);
        saveSteps(flow.getId(), req.getSteps());
        return toDto(flow);
    }

    @Override
    @Transactional
    public ApprovalFlowDto update(String id, ApprovalFlowRequest req) {
        ApprovalFlow flow = flowRepository.findById(id)
                .filter(f -> Boolean.FALSE.equals(f.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Flow không tồn tại"));
        if (req.getName() != null) flow.setName(req.getName());
        if (req.getSubjectType() != null) flow.setSubjectType(req.getSubjectType());
        if (req.getActive() != null) flow.setActive(req.getActive());
        if (req.getDescription() != null) flow.setDescription(req.getDescription());
        flowRepository.save(flow);
        if (req.getSteps() != null) {
            flowStepRepository.findByFlowIdAndIsDeletedFalseOrderByStepOrderAsc(id)
                    .forEach(s -> {
                        s.setIsDeleted(true);
                        flowStepRepository.save(s);
                    });
            saveSteps(id, req.getSteps());
        }
        return toDto(flow);
    }

    private void saveSteps(String flowId, List<ApprovalFlowStepTemplate> steps) {
        if (steps == null) return;
        for (ApprovalFlowStepTemplate t : steps) {
            ApprovalFlowStep step = ApprovalFlowStep.builder()
                    .flowId(flowId)
                    .stepOrder(t.getStepOrder())
                    .approverRole(t.getApproverRole())
                    .name(t.getLabel())
                    .build();
            step.setId(UUID.randomUUID().toString());
            flowStepRepository.save(step);
        }
    }

    private ApprovalFlowDto toDto(ApprovalFlow f) {
        List<ApprovalFlowStepTemplate> steps = flowStepRepository
                .findByFlowIdAndIsDeletedFalseOrderByStepOrderAsc(f.getId())
                .stream()
                .map(s -> ApprovalFlowStepTemplate.builder()
                        .stepOrder(s.getStepOrder())
                        .approverRole(s.getApproverRole())
                        .label(s.getName())
                        .build())
                .toList();
        return ApprovalFlowDto.builder()
                .id(f.getId())
                .name(f.getName())
                .subjectType(f.getSubjectType())
                .steps(steps)
                .active(Boolean.TRUE.equals(f.getActive()))
                .description(f.getDescription())
                .createdAt(f.getCreatedDate() != null ? f.getCreatedDate().format(ISO) : null)
                .updatedAt(f.getUpdatedDate() != null ? f.getUpdatedDate().format(ISO) : null)
                .build();
    }
}
