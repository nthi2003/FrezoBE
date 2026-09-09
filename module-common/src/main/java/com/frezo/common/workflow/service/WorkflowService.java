package com.frezo.common.workflow.service;

import com.frezo.common.workflow.dto.WorkflowDefinitionDto;
import com.frezo.common.workflow.dto.WorkflowInstanceDto;
import com.frezo.common.workflow.dto.WorkflowTaskDto;

import java.util.List;
import java.util.Optional;

/**
 * Public API cho workflow engine.
 * <p>
 * Modules khác wire vào bằng cách gọi {@link #start(String, String, String, String, String)}
 * khi tạo entity, và {@link #findInstanceByEntity(String, String)} để render progress.
 * Action approve/reject/cancel expose qua REST — mọi module dùng chung.
 */
public interface WorkflowService {


    List<WorkflowDefinitionDto> listDefinitions(String moduleCode);

    WorkflowDefinitionDto getDefinition(String id);

    WorkflowDefinitionDto getDefinitionByCode(String code);

    WorkflowDefinitionDto saveDefinition(WorkflowDefinitionDto dto);

    void deleteDefinition(String id);

    WorkflowInstanceDto start(String definitionCode, String entityType, String entityId,
                              String startedBy, String title);


    Optional<WorkflowInstanceDto> findInstanceByEntity(String entityType, String entityId);


    WorkflowInstanceDto cancelInstance(String instanceId);


    List<WorkflowTaskDto> myPendingTasks();

    WorkflowTaskDto approveTask(String taskId, String comment);

    WorkflowTaskDto rejectTask(String taskId, String reason);
}
