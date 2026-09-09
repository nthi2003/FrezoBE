package com.frezo.approval.service.impl;

import com.frezo.approval.dto.ApprovalRequestDto;
import com.frezo.approval.dto.ApprovalStepDto;
import com.frezo.approval.dto.CreateApprovalRequest;
import com.frezo.approval.entity.ApprovalRequest;
import com.frezo.approval.entity.ApprovalStep;
import com.frezo.approval.event.ApprovalDecidedEvent;
import com.frezo.approval.repository.ApprovalRequestRepository;
import com.frezo.approval.repository.ApprovalStepRepository;
import com.frezo.approval.service.ApprovalCreator;
import com.frezo.approval.service.ApprovalService;
import com.frezo.approval.service.ApproverResolver;
import com.frezo.common.exception.AppException;
import com.frezo.common.exception.CommonErrorCode;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.response.FePage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ApprovalRequestRepository requestRepository;
    private final ApprovalStepRepository stepRepository;
    private final ApproverResolver approverResolver;
    private final ApprovalCreator approvalCreator;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Liệt kê các yêu cầu phê duyệt liên quan đến người dùng hiện tại.
     * Mặc định chỉ hiển thị các yêu cầu đang chờ (PENDING).
     */
    @Override
    public FePage<ApprovalRequestDto> listMy(String status) {
        String me = SystemUtils.getCurrentUsername();
        boolean pendingOnly = status == null || "pending".equalsIgnoreCase(status);
        List<ApprovalRequest> all = pendingOnly
                ? requestRepository.findByStatusAndIsDeletedFalseOrderByRequestedAtDesc("PENDING")
                : requestRepository.findByIsDeletedFalseOrderByRequestedAtDesc();

        List<ApprovalRequestDto> mine = all.stream()
                .filter(r -> pendingOnly ? isCurrentApprover(r, me) : isRelated(r, me))
                .map(this::toDto)
                .toList();
        return FePage.all(mine);
    }

    /**
     * Phê duyệt yêu cầu ở bước hiện tại, nếu là bước cuối thì đóng yêu cầu.
     */
    @Override
    @Transactional
    public ApprovalRequestDto approve(String id, String comment) {
        return action(id, comment, true);
    }

    /**
     * Từ chối yêu cầu phê duyệt, cập nhật trạng thái REJECTED.
     */
    @Override
    @Transactional
    public ApprovalRequestDto reject(String id, String comment) {
        return action(id, comment, false);
    }

    /**
     * Lấy dòng thời gian các bước phê duyệt của một yêu cầu.
     */
    @Override
    public List<ApprovalStepDto> timeline(String id) {
        findRequest(id);
        return stepRepository.findByRequestIdAndIsDeletedFalseOrderByStepOrderAsc(id)
                .stream().map(this::toStepDto).toList();
    }

    /**
     * Lấy dòng thời gian phê duyệt theo đối tượng (subjectType + subjectId).
     */
    @Override
    public List<ApprovalStepDto> timelineBySubject(String subjectType, String subjectId) {
        return requestRepository
                .findFirstBySubjectTypeAndSubjectIdAndIsDeletedFalseOrderByRequestedAtDesc(subjectType, subjectId)
                .map(r -> timeline(r.getId()))
                .orElse(List.of());
    }

    /**
     * Tạo mới yêu cầu phê duyệt và phát sự kiện ASSIGNED để thông báo bước đầu tiên.
     */
    @Override
    @Transactional
    public ApprovalRequestDto create(CreateApprovalRequest req) {
        ApprovalRequest saved = approvalCreator.create(
                req.getSubjectType(), req.getSubjectId(), req.getSubjectSummary(),
                req.getFlowId(), req.getFlowCode(), req.getRequestedBy());
        eventPublisher.publishEvent(new ApprovalDecidedEvent(
                this, saved.getId(), saved.getSubjectType(), saved.getSubjectId(),
                "ASSIGNED", saved.getRequestedBy(), null));
        return toDto(saved);
    }

    /**
     * Tìm yêu cầu phê duyệt theo đối tượng (subjectType + subjectId), lấy bản gần nhất.
     */
    @Override
    public ApprovalRequestDto findBySubject(String subjectType, String subjectId) {
        return requestRepository
                .findFirstBySubjectTypeAndSubjectIdAndIsDeletedFalseOrderByRequestedAtDesc(subjectType, subjectId)
                .map(this::toDto)
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND,
                        "Không có approval cho subject"));
    }

    /** Xử lý chung cho approve/reject: kiểm tra quyền, cập nhật bước và chuyển tiếp yêu cầu. */
    private ApprovalRequestDto action(String id, String comment, boolean approve) {
        String me = SystemUtils.getCurrentUsername();
        ApprovalRequest req = findRequest(id);
        if (!"PENDING".equals(req.getStatus())) {
            throw new AppException(CommonErrorCode.CONFLICT, "Request không còn PENDING");
        }
        if (!isCurrentApprover(req, me)) {
            throw new AppException(CommonErrorCode.FORBIDDEN, "Bạn không phải approver bước hiện tại");
        }

        ApprovalStep current = stepRepository
                .findByRequestIdAndStepOrderAndIsDeletedFalse(req.getId(), req.getCurrentStepOrder())
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Không tìm thấy bước duyệt"));

        current.setAction(approve ? "APPROVED" : "REJECTED");
        current.setComment(comment);
        current.setActionedAt(LocalDateTime.now());
        current.setApproverUsername(me);
        current.setApproverName(approverResolver.displayName(me));
        stepRepository.save(current);

        if (!approve) {
            req.setStatus("REJECTED");
            requestRepository.save(req);
            eventPublisher.publishEvent(new ApprovalDecidedEvent(
                    this, req.getId(), req.getSubjectType(), req.getSubjectId(),
                    "REJECTED", me, comment));
            return toDto(req);
        }

        List<ApprovalStep> steps = stepRepository
                .findByRequestIdAndIsDeletedFalseOrderByStepOrderAsc(req.getId());
        ApprovalStep next = steps.stream()
                .filter(s -> s.getStepOrder() > req.getCurrentStepOrder())
                .filter(s -> "PENDING".equals(s.getAction()))
                .findFirst()
                .orElse(null);

        if (next == null) {
            req.setStatus("APPROVED");
            requestRepository.save(req);
            eventPublisher.publishEvent(new ApprovalDecidedEvent(
                    this, req.getId(), req.getSubjectType(), req.getSubjectId(),
                    "APPROVED", me, comment));
        } else {
            req.setCurrentStepOrder(next.getStepOrder());
            req.setCurrentApproverHint(next.getApproverRole());
            requestRepository.save(req);
            eventPublisher.publishEvent(new ApprovalDecidedEvent(
                    this, req.getId(), req.getSubjectType(), req.getSubjectId(),
                    "ASSIGNED", me, null));
        }
        return toDto(req);
    }

    /** Kiểm tra người dùng có phải approver hợp lệ của bước hiện tại không. */
    private boolean isCurrentApprover(ApprovalRequest r, String username) {
        if (!"PENDING".equals(r.getStatus())) return false;
        return stepRepository
                .findByRequestIdAndStepOrderAndIsDeletedFalse(r.getId(), r.getCurrentStepOrder())
                .map(s -> {
                    if (username != null && username.equalsIgnoreCase(s.getApproverUsername())) return true;
                    return approverResolver.userHasRole(username, s.getApproverRole());
                })
                .orElse(false);
    }

    /** Kiểm tra người dùng có liên quan đến yêu cầu (người yêu cầu hoặc approver ở bất kỳ bước nào). */
    private boolean isRelated(ApprovalRequest r, String username) {
        if (username != null && username.equalsIgnoreCase(r.getRequestedBy())) return true;
        return stepRepository.findByRequestIdAndIsDeletedFalseOrderByStepOrderAsc(r.getId()).stream()
                .anyMatch(s -> username != null && (
                        username.equalsIgnoreCase(s.getApproverUsername())
                                || approverResolver.userHasRole(username, s.getApproverRole())));
    }

    /** Tìm yêu cầu phê duyệt theo mã, ném ngoại lệ nếu không tồn tại hoặc đã bị xóa. */
    private ApprovalRequest findRequest(String id) {
        return requestRepository.findById(id)
                .filter(r -> Boolean.FALSE.equals(r.getIsDeleted()))
                .orElseThrow(() -> new AppException(CommonErrorCode.NOT_FOUND, "Approval request không tồn tại"));
    }

    /** Chuyển entity yêu cầu phê duyệt sang DTO kèm tên người yêu cầu hiển thị. */
    private ApprovalRequestDto toDto(ApprovalRequest r) {
        return ApprovalRequestDto.builder()
                .id(r.getId())
                .subjectType(r.getSubjectType())
                .subjectId(r.getSubjectId())
                .subjectSummary(r.getSubjectSummary())
                .currentStep(r.getCurrentStepOrder())
                .totalSteps(r.getTotalSteps())
                .requestedBy(r.getRequestedBy())
                .requestedByName(approverResolver.displayName(r.getRequestedBy()))
                .requestedAt(r.getRequestedAt() != null ? r.getRequestedAt().format(ISO) : null)
                .status(r.getStatus())
                .currentApproverHint(r.getCurrentApproverHint())
                .build();
    }

    /** Chuyển entity bước phê duyệt sang DTO. */
    private ApprovalStepDto toStepDto(ApprovalStep s) {
        return ApprovalStepDto.builder()
                .id(s.getId())
                .stepOrder(s.getStepOrder())
                .approverName(s.getApproverName() != null ? s.getApproverName() : s.getApproverRole())
                .approverUsername(s.getApproverUsername())
                .action(s.getAction())
                .comment(s.getComment())
                .actionedAt(s.getActionedAt() != null ? s.getActionedAt().format(ISO) : null)
                .build();
    }
}
