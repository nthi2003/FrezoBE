package com.frezo.approval.dto;

import lombok.Data;

import java.util.List;

@Data
public class ApprovalFlowRequest {
    private String name;
    private String subjectType;
    private List<ApprovalFlowStepTemplate> steps;
    private Boolean active;
    private String description;
}
