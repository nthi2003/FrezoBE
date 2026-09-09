package com.frezo.common.workflow.dto;

import lombok.Data;

import java.util.List;

@Data
public class WorkflowInstanceDto {
    private String id;
    private String definitionCode;
    private String definitionName;
    private String entityType;
    private String entityId;
    private String title;
    private String startedBy;
    private String startedAt;
    private Integer currentStep;
    private String status;
    private String completedAt;
    private List<WorkflowTaskDto> tasks;
    private List<WorkflowStepDto> steps;
}
