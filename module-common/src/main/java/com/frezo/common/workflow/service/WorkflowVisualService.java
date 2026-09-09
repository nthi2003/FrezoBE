package com.frezo.common.workflow.service;

import com.frezo.common.workflow.dto.WorkflowDefinitionDto;
import com.frezo.common.workflow.dto.WorkflowGraphDto;
import com.frezo.common.workflow.dto.WorkflowTemplateMetaDto;
import com.frezo.common.workflow.dto.WorkflowValidateResultDto;

import java.util.List;


public interface WorkflowVisualService {

    List<WorkflowTemplateMetaDto> listTemplates();

    WorkflowDefinitionDto getTemplate(String code);


    WorkflowDefinitionDto cloneTemplate(String code);

    WorkflowDefinitionDto getDefinitionVisual(String id);

    WorkflowDefinitionDto updateDefinitionVisual(String id, WorkflowDefinitionDto dto);

    WorkflowGraphDto getGraph(String definitionId);

    WorkflowGraphDto saveGraph(String definitionId, WorkflowGraphDto graph);

    WorkflowValidateResultDto validate(String definitionId);
}
