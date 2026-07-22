package com.frezo.approval.dto;

import lombok.Data;

@Data
public class CreateApprovalRequest {
    private String subjectType;
    private String subjectId;
    private String subjectSummary;
    private String flowId;
    /** Ưu tiên sau flowId — VD LEAVE_STANDARD, PAYROLL_PERIOD, PURCHASE_REQUEST. */
    private String flowCode;
    private String requestedBy;
}
