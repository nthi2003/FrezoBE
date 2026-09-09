package com.frezo.common.workflow.dto;

import lombok.Data;

@Data
public class WorkflowTaskDto {
    private String id;
    private String instanceId;
    private Integer stepOrder;
    private String stepName;
    private String approverType;
    private String assigneeUsername;
    private String assigneeRole;
    private String status;
    private String decidedBy;
    private String decidedAt;
    private String comment;
    private String deadline;
    private String createdDate;
    private String entityType;
    private String entityId;
    private String instanceTitle;
    private String instanceStartedBy;
}
