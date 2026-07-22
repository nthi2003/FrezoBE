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

    // ============================================================
    // Definitions — Admin CRUD
    // ============================================================

    List<WorkflowDefinitionDto> listDefinitions(String moduleCode);

    WorkflowDefinitionDto getDefinition(String id);

    WorkflowDefinitionDto getDefinitionByCode(String code);

    WorkflowDefinitionDto saveDefinition(WorkflowDefinitionDto dto);

    void deleteDefinition(String id);

    // ============================================================
    // Instances — được gọi từ các module business
    // ============================================================

    /**
     * Khởi tạo instance mới cho 1 entity + tạo task cho step đầu tiên.
     *
     * @param definitionCode mã definition (VD "ASSET_TRANSFER_DEFAULT")
     * @param entityType     loại entity (VD "ASSET_TRANSFER")
     * @param entityId       PK của entity
     * @param startedBy      username khởi tạo — thường là requester
     * @param title          Nullable — tiêu đề hiển thị trong inbox
     * @return instance vừa tạo (đã có task đầu tiên)
     */
    WorkflowInstanceDto start(String definitionCode, String entityType, String entityId,
                              String startedBy, String title);

    /** Lookup instance theo entity — dùng cho FE render progress bar. */
    Optional<WorkflowInstanceDto> findInstanceByEntity(String entityType, String entityId);

    /** Huỷ instance đang chạy — do requester hoặc admin. */
    WorkflowInstanceDto cancelInstance(String instanceId);

    // ============================================================
    // Tasks — inbox + actions
    // ============================================================

    /** Task PENDING cho user hiện tại (assign trực tiếp + role pool + admin). */
    List<WorkflowTaskDto> myPendingTasks();

    WorkflowTaskDto approveTask(String taskId, String comment);

    WorkflowTaskDto rejectTask(String taskId, String reason);
}
