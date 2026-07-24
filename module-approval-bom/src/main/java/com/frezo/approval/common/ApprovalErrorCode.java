package com.frezo.approval.common;

import com.frezo.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Error codes cho Approval Engine (LNK-05 hard-fail khi thiếu flow).
 */
@Getter
@RequiredArgsConstructor
public enum ApprovalErrorCode implements ErrorCode {

    FLOW_NOT_FOUND("approval.flow.not.found", HttpStatus.BAD_REQUEST,
            "Không tìm thấy quy trình duyệt (flow). Kiểm tra seed LEAVE_STANDARD / PAYROLL_PERIOD / PURCHASE_REQUEST."),
    FLOW_EMPTY("approval.flow.empty", HttpStatus.BAD_REQUEST,
            "Quy trình duyệt chưa có bước — không thể tạo yêu cầu duyệt."),
    /** Role bước duyệt không có User nào → tránh phiếu PENDING treo không vào Inbox. */
    NO_APPROVER("approval.no.approver", HttpStatus.BAD_REQUEST,
            "Không có người duyệt cho vai trò {0}. Gán User+Role (QTHT) trước khi gửi duyệt.");

    private final String key;
    private final HttpStatus status;
    private final String defaultMessage;
}
