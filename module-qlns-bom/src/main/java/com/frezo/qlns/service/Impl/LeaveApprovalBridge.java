package com.frezo.qlns.service.impl;

import com.frezo.approval.entity.ApprovalRequest;
import com.frezo.approval.service.ApprovalCreator;
import com.frezo.common.domain.SubjectType;
import com.frezo.common.exception.AppException;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qlns.common.QlnsErrorCode;
import com.frezo.qlns.common.StatusContarct;
import com.frezo.qlns.entity.Contract;
import com.frezo.qlns.entity.LeaveRequest;
import com.frezo.qlns.repository.ContractRepository;
import com.frezo.qlns.repository.LeaveRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Bridge Approval + LeaveApprovalResolver — giữ LeaveRequestServiceImpl ≤5 deps.
 * Runtime: dùng flow LEAVE đang active tại /approval/flows (không hardcode code).
 */
@Component
@RequiredArgsConstructor
public class LeaveApprovalBridge {

    /** Seed code tham chiếu — runtime resolve theo subjectType LEAVE + active. */
    public static final String FLOW_CODE = "LEAVE_STANDARD";

    private final ApprovalCreator approvalCreator;
    private final LeaveRequestRepository leaveRequestRepository;
    private final LeaveApprovalResolver approvalResolver;
    private final ContractRepository contractRepository;

    /**
     * Kiểm tra hợp đồng tồn tại và đang ở trạng thái activated + ACTIVE trước khi tạo đơn nghỉ,
     * nếu không thoả mãn sẽ ném lỗi nghiệp vụ để chặn tạo đơn.
     */
    public void assertActiveContract(String contractId) {
        if (contractId == null || contractId.isBlank()) {
            throw new AppException("leave.contract.required", HttpStatus.BAD_REQUEST);
        }
        Contract c = contractRepository.findById(contractId)
                .orElseThrow(() -> new AppException("leave.contract.required", HttpStatus.BAD_REQUEST));
        if (!Boolean.TRUE.equals(c.getActivated()) || c.getStatus() != StatusContarct.ACTIVE) {
            throw new AppException("leave.contract.not.active", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Khởi tạo luồng duyệt (approval request) cho đơn nghỉ và lưu lại approvalRequestId
     * vào đơn nghỉ tương ứng trong DB.
     */
    @Transactional
    public void start(LeaveRequest leave) {
        String summary = String.format("%s %s → %s (%s ngày)",
                leave.getLeaveType(),
                leave.getStartDate(),
                leave.getEndDate(),
                leave.getDurationDays() != null ? leave.getDurationDays() : "?");
        // null flowId/flowCode → ApprovalCreator lấy flow LEAVE active (draft đã kích hoạt)
        ApprovalRequest req = approvalCreator.create(
                SubjectType.LEAVE.name(),
                leave.getId(),
                summary,
                null,
                null,
                null);
        leave.setApprovalRequestId(req.getId());
        leaveRequestRepository.save(leave);
    }

    // ============================================================
    // IDOR guard — đơn nghỉ chỉ của chính mình, trừ khi là người duyệt
    // ============================================================

    /**
     * Chặn đọc đơn nghỉ của người khác qua {@code GET /qlns/leave-request/my/{contractId}}.
     * <p>Cho qua khi: admin / HR / có quyền duyệt (xem {@link LeaveApprovalResolver#canViewOthersLeave()}),
     * hoặc {@code contractId} thuộc đúng hồ sơ nhân sự của current user.
     *
     * @throws AppException 403 khi không sở hữu HĐ, 404 khi HĐ không tồn tại
     */
    public void assertCanViewContractLeaves(String contractId) {
        if (contractId == null || contractId.isBlank()) {
            throw new AppException(QlnsErrorCode.CONTRACT_NOT_FOUND);
        }
        if (approvalResolver.canViewOthersLeave()) return;

        String me = approvalResolver.currentPersonId();
        if (me == null) {
            throw new AppException(QlnsErrorCode.LEAVE_REQUEST_VIEW_DENIED);
        }
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new AppException(QlnsErrorCode.CONTRACT_NOT_FOUND));
        if (!me.equals(contract.getPersonId())) {
            throw new AppException(QlnsErrorCode.LEAVE_REQUEST_VIEW_DENIED);
        }
    }

    /**
     * Chặn đọc chi tiết / timeline đơn nghỉ của người khác.
     * <p>Cho qua khi: admin / HR / có quyền duyệt, người tạo đơn, QL đang được giao duyệt đơn,
     * hoặc đơn thuộc chính hồ sơ nhân sự của current user.
     */
    public void assertCanViewLeave(LeaveRequest leave) {
        if (leave == null) {
            throw new AppException(QlnsErrorCode.LEAVE_REQUEST_NOT_FOUND);
        }
        if (approvalResolver.canViewOthersLeave()) return;

        String actor = SystemUtils.getCurrentUsername();
        if (actor != null
                && (actor.equals(leave.getCreatedBy()) || actor.equals(leave.getManagerUsername()))) {
            return;
        }
        String me = approvalResolver.currentPersonId();
        if (me != null && (me.equals(leave.getPersonId()) || ownsContract(me, leave.getContractId()))) {
            return;
        }
        throw new AppException(QlnsErrorCode.LEAVE_REQUEST_VIEW_DENIED);
    }

    /** HĐ legacy chưa set personId trên đơn → đối chiếu qua Contract. */
    private boolean ownsContract(String personId, String contractId) {
        if (contractId == null || contractId.isBlank()) return false;
        return contractRepository.findById(contractId)
                .map(c -> personId.equals(c.getPersonId()))
                .orElse(false);
    }

    /**
     * Uỷ quyền tìm username quản lý trực tiếp của nhân viên cho resolver.
     */
    public String resolveManagerUsername(String personId) {
        return approvalResolver.resolveManagerUsername(personId);
    }

    /**
     * Uỷ quyền kiểm tra current user có phải admin hay không.
     */
    public boolean isCurrentUserAdmin() {
        return approvalResolver.isCurrentUserAdmin();
    }

    /**
     * Uỷ quyền lấy danh sách username HR nhận thông báo duyệt đơn.
     */
    public List<String> resolveHrUsernames() {
        return approvalResolver.resolveHrUsernames();
    }
}
