package com.frezo.qtbv.dto.response;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AssetTransferRequestResponse {
    private String id;
    private String assetId;
    private String assetCode;
    private String assetName;
    private String requestType;
    private String status;
    private String requesterUsername;
    private String personId;
    private String personName;
    private String reason;
    private LocalDate plannedDate;
    private String approvedBy;
    private String approvedAt;
    private String approveNote;
    private String rejectedBy;
    private String rejectedAt;
    private String rejectReason;
    private String cancelledAt;
    private String handedOverBy;
    private String handedOverAt;
    private String handoverNote;
    private String createdBy;
    private String createdDate;
    private String workflowInstanceId;
    private String workflowEntityType;
    private String currentTaskId;
    private String currentStepName;
}
