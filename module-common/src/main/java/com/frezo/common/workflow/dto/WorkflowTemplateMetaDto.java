package com.frezo.common.workflow.dto;

import lombok.Data;

/**
 * Metadata mẫu trong gallery — FE không hardcode graph.
 * Field {@code key} khớp FE {@code WorkflowTemplateMeta.key}.
 */
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
