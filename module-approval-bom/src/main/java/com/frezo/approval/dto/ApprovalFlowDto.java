package com.frezo.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalFlowDto {
    private String id;
    private String name;
    private String subjectType;
    private List<ApprovalFlowStepTemplate> steps;
    private Boolean active;
    private String description;
    private String createdAt;
    private String updatedAt;
}
