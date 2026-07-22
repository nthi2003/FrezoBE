package com.frezo.common.workflow.dto;

import lombok.Data;

import java.util.List;

/**
 * Full DTO cho editor UI — bao gồm cả steps để 1 request save đầy đủ.
 * <p>
 * FE gửi POST/PUT với payload này; BE xoá và insert lại toàn bộ steps (upsert simple).
 */
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

    /** SIMPLE | VISUAL — default SIMPLE cho API /wf cũ. */
    private String editorMode;
    /** Graph visual — object JSON (lanes/nodes/edges). */
    private WorkflowGraphDto graphJson;
    private String guideMarkdown;
    private String templateKey;
    private String sourceTemplateCode;
    private Integer version;
    private Boolean isTemplate;
}
