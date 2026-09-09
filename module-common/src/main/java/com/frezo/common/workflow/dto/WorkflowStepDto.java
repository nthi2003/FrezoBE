package com.frezo.common.workflow.dto;

import lombok.Data;

@Data
public class WorkflowStepDto {
    private String id;
    private Integer stepOrder;
    private String stepName;
    private String approverType;
    private String approverValue;
    private Boolean allowSkip;
    private Integer slaHours;
    private String description;
}
