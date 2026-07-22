package com.frezo.qlns.service.impl;

import com.frezo.approval.entity.ApprovalRequest;
import com.frezo.approval.service.ApprovalCreator;
import com.frezo.common.domain.SubjectType;
import com.frezo.qlns.entity.LeaveRequest;
import com.frezo.qlns.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Bridge Approval + LeaveApprovalResolver — giữ LeaveRequestServiceImpl ≤5 deps.
 */
@Component
@RequiredArgsConstructor
public class LeaveApprovalBridge {

    public static final String FLOW_CODE = "LEAVE_STANDARD";

    private final ApprovalCreator approvalCreator;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveApprovalResolver approvalResolver;

    @Transactional
    public void start(LeaveRequest leave) {
        String summary = String.format("%s %s → %s (%s ngày)",
                leave.getLeaveType(),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getDurationDays() != null ? leave.getDurationDays() : "?");
        ApprovalRequest req = approvalCreator.create(
                SubjectType.LEAVE.name(),
                leave.getId(),
                summary,
                null,
                FLOW_CODE,
                null);
        leave.setApprovalRequestId(req.getId());
        leaveRequestRepository.save(leave);
    }

    public String resolveManagerUsername(String personId) {
        return approvalResolver.resolveManagerUsername(personId);
    }

    public boolean isCurrentUserAdmin() {
        return approvalResolver.isCurrentUserAdmin();
    }

    public List<String> resolveHrUsernames() {
        return approvalResolver.resolveHrUsernames();
    }
}
