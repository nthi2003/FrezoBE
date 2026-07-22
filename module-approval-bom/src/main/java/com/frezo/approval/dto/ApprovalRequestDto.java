package com.frezo.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequestDto {
    private String id;
    private String subjectType;
    private String subjectId;
    private String subjectSummary;
    private Integer currentStep;
    private Integer totalSteps;
    private String requestedBy;
    private String requestedByName;
    private String requestedAt;
    private String status;
    private String currentApproverHint;
}
