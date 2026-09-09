package com.frezo.common.workflow.dto;

import lombok.Data;

import java.util.List;

@Data
public class WorkflowDefinitionDto {
    private String id;
    private String code;
    private String name;
    private String moduleCode;
    private String description;
    private Boolean active;
    private List<WorkflowStepDto> steps;
    private String createdBy;
    private String createdDate;
    private String editorMode;
    private WorkflowGraphDto graphJson;
    private String guideMarkdown;
    private String templateKey;
    private String sourceTemplateCode;
    private Integer version;
    private Boolean isTemplate;
}
