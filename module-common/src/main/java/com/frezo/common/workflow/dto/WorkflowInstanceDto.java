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
    /** Toàn bộ tasks đã tạo (đã hoàn tất + đang xử lý) để render stepper. */
    private List<WorkflowTaskDto> tasks;
    /** Snapshot template steps (từ definition) để FE render step name kể cả bước chưa tạo task. */
    private List<WorkflowStepDto> steps;
}
