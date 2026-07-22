package com.frezo.qlns.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class LeaveRequestResponse {
    private String id;
    private String contractId;
    private String personId;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double durationDays;
    private String reason;

    /** PENDING_MANAGER | PENDING_HR | APPROVED | REJECTED | CANCELLED */
    private String status;

    /** Username QL trực tiếp — resolve từ dept.managerId lúc create. */
    private String managerUsername;

    private String attachmentUrl;

    // ---- Manager approval ----
    private String managerApprovedBy;
    private LocalDate managerApprovedAt;

    // ---- HR approval ----
    private String hrApprovedBy;
    private LocalDate hrApprovedAt;

    // ---- Rejection ----
    private String rejectedBy;
    private LocalDate rejectedAt;
    private String rejectReason;

    // ---- Legacy (deprecated) ----
    private String approvedBy;
    private LocalDate approvedAt;

    // ---- Audit ----
    private String createdBy;
    private String createdDate;

    /** ID ApprovalRequest (module-approval). */
    private String approvalRequestId;
}
