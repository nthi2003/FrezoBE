package com.frezo.approval.listener;

import com.frezo.approval.entity.ApprovalRequest;
import com.frezo.approval.event.ApprovalDecidedEvent;
import com.frezo.approval.repository.ApprovalRequestRepository;
import com.frezo.approval.repository.ApprovalStepRepository;
import com.frezo.approval.service.ApproverResolver;
import com.frezo.common.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Gửi notification khi assign / decide — tách khỏi ApprovalService để giữ ≤5 deps.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalNotificationListener {

    private final NotificationService notificationService;
    private final ApprovalRequestRepository requestRepository;
    private final ApprovalStepRepository stepRepository;
    private final ApproverResolver approverResolver;

    @EventListener
    public void onDecided(ApprovalDecidedEvent event) {
        try {
            ApprovalRequest req = requestRepository.findById(event.getRequestId()).orElse(null);
            if (req == null) return;

            String deepLink = deepLink(req.getSubjectType(), req.getSubjectId(), req.getId());

            if ("ASSIGNED".equals(event.getStatus())) {
                notifyAssignees(req, deepLink);
            } else if ("APPROVED".equals(event.getStatus()) || "REJECTED".equals(event.getStatus())) {
                notificationService.notify(
                        req.getRequestedBy(),
                        "Yêu cầu duyệt " + event.getStatus(),
                        req.getSubjectSummary() != null ? req.getSubjectSummary() : req.getSubjectId(),
                        "APPROVAL_" + event.getStatus(),
                        req.getSubjectType(),
                        req.getSubjectId(),
                        deepLink,
                        event.getActedBy(),
                        false);
            }
        } catch (Exception e) {
            log.warn("[Approval] notify failed: {}", e.getMessage());
        }
    }

    private void notifyAssignees(ApprovalRequest req, String deepLink) {
        stepRepository.findByRequestIdAndStepOrderAndIsDeletedFalse(req.getId(), req.getCurrentStepOrder())
                .ifPresent(step -> {
                    List<String> usernames;
                    if (step.getApproverUsername() != null) {
                        usernames = List.of(step.getApproverUsername());
                    } else {
                        usernames = approverResolver.resolveAll(step.getApproverRole()).stream()
                                .map(ApproverResolver.ApproverHint::username)
                                .toList();
                    }
                    notificationService.notifyMany(
                            usernames,
                            "Có yêu cầu cần duyệt",
                            req.getSubjectSummary() != null ? req.getSubjectSummary() : req.getSubjectId(),
                            "APPROVAL_ASSIGNED",
                            req.getSubjectType(),
                            req.getSubjectId(),
                            deepLink,
                            req.getRequestedBy(),
                            false);
                });
    }

    static String deepLink(String subjectType, String subjectId, String approvalId) {
        if (subjectType == null) return "/approvals?id=" + approvalId;
        return switch (subjectType) {
            case "LEAVE" -> "/qlns/leaves?highlight=" + subjectId;
            case "PAYROLL", "PAYROLL_PERIOD" -> "/qlns/payroll-periods?id=" + subjectId;
            case "PURCHASE_REQUEST" -> "/warehouse/purchase-requests?id=" + subjectId;
            default -> "/approvals?id=" + approvalId;
        };
    }
}
