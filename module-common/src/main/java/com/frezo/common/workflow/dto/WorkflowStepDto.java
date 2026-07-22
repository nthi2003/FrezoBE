package com.frezo.common.workflow.dto;

import lombok.Data;

/**
 * DTO 2 chiều — dùng cả request (từ editor UI) lẫn response (render steps).
 */
@Data
public class WorkflowStepDto {
    private String id;
    private Integer stepOrder;
    private String stepName;
    /** USER | ROLE | MANAGER | ADMIN */
    private String approverType;
    private String approverValue;
    private Boolean allowSkip;
    private Integer slaHours;
    private String description;
}
