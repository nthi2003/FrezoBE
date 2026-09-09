package com.frezo.common.workflow.dto;

import lombok.Data;


@Data
public class WorkflowTemplateMetaDto {
    private String key;
    private String name;
    private String description;
    private String moduleCode;
    private Integer nodeCount;
    private String updatedAt;
    private String guideMarkdown;
    private Integer version;
}
