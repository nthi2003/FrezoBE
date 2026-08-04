package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.workflow.dto.WorkflowDefinitionDto;
import com.frezo.common.workflow.service.WorkflowService;
import com.frezo.common.security.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST cho Workflow Engine — dùng chung cho toàn hệ thống.
 * <p>
 * Endpoint chia làm 3 nhóm:
 * <ul>
 *   <li>{@code /wf/definitions} — Admin CRUD template quy trình</li>
 *   <li>{@code /wf/instances}   — Query state instance theo entity (dùng cho FE render progress)</li>
 *   <li>{@code /wf/tasks}       — Inbox + approve/reject cho user hiện tại</li>
 * </ul>
 */
@RestController
@RequestMapping("/wf")
@RequiredArgsConstructor
@Tag(name = "99. Workflow Engine", description = "Quản lý quy trình duyệt chung cho mọi module")
public class WorkflowController {

    private final WorkflowService workflowService;

    // ---- Definitions ----

    @Operation(summary = "Danh sách definition (filter theo moduleCode)")
    @GetMapping("/definitions")
    @CheckPermission(api = "/wf/definitions", action = "VIEW")
    public ApiResponse<?> listDefinitions(@RequestParam(required = false) String moduleCode) {
        return ApiResponse.success(workflowService.listDefinitions(moduleCode));
    }

    @Operation(summary = "Chi tiết definition (kèm steps)")
    @GetMapping("/definitions/{id}")
    @CheckPermission(api = "/wf/definitions/{id}", action = "VIEW")
    public ApiResponse<?> getDefinition(@PathVariable String id) {
        return ApiResponse.success(workflowService.getDefinition(id));
    }

    @Operation(summary = "Lookup định nghĩa theo code (cho module tích hợp)")
    @GetMapping("/definitions/by-code/{code}")
    @CheckPermission(api = "/wf/definitions/by-code/{code}", action = "VIEW")
    public ApiResponse<?> getDefinitionByCode(@PathVariable String code) {
        return ApiResponse.success(workflowService.getDefinitionByCode(code));
    }

    @Operation(summary = "Tạo / cập nhật definition (upsert steps)")
    @PostMapping("/definitions")
    @CheckPermission(api = "/wf/definitions", action = "CREATE")
    public ApiResponse<?> saveDefinition(@RequestBody WorkflowDefinitionDto dto) {
        return ApiResponse.success(workflowService.saveDefinition(dto));
    }

    @Operation(summary = "Xoá mềm definition")
    @DeleteMapping("/definitions/{id}")
    @CheckPermission(api = "/wf/definitions/{id}", action = "DELETE")
    public ApiResponse<?> deleteDefinition(@PathVariable String id) {
        workflowService.deleteDefinition(id);
        return ApiResponse.success(null);
    }

    // ---- Instances ----

    @Operation(summary = "Trạng thái workflow của 1 entity (cho FE render progress)")
    @GetMapping("/instances/by-entity/{entityType}/{entityId}")
    @CheckPermission(api = "/wf/instances/by-entity/{entityType}/{entityId}", action = "VIEW")
    public ApiResponse<?> getInstanceByEntity(@PathVariable String entityType, @PathVariable String entityId) {
        return ApiResponse.success(
                workflowService.findInstanceByEntity(entityType, entityId).orElse(null));
    }

    @Operation(summary = "Huỷ 1 instance đang chạy")
    @PostMapping("/instances/{id}/cancel")
    @CheckPermission(api = "/wf/instances/{id}/cancel", action = "UPDATE")
    public ApiResponse<?> cancelInstance(@PathVariable String id) {
        return ApiResponse.success(workflowService.cancelInstance(id));
    }

    // ---- Tasks ----

    @Operation(summary = "Task chờ duyệt của user hiện tại (inbox)")
    @GetMapping("/tasks/mine")
    @CheckPermission(api = "/wf/tasks/mine", action = "VIEW")
    public ApiResponse<?> myPendingTasks() {
        return ApiResponse.success(workflowService.myPendingTasks());
    }

    @Operation(summary = "Duyệt 1 task")
    @PostMapping("/tasks/{id}/approve")
    @CheckPermission(api = "/wf/tasks/{id}/approve", action = "APPROVE")
    public ApiResponse<?> approveTask(@PathVariable String id,
                                      @RequestBody(required = false) Map<String, String> body) {
        String comment = body != null ? body.getOrDefault("comment", null) : null;
        return ApiResponse.success(workflowService.approveTask(id, comment));
    }

    @Operation(summary = "Từ chối 1 task")
    @PostMapping("/tasks/{id}/reject")
    @CheckPermission(api = "/wf/tasks/{id}/reject", action = "APPROVE")
    public ApiResponse<?> rejectTask(@PathVariable String id, @RequestBody Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", null) : null;
        return ApiResponse.success(workflowService.rejectTask(id, reason));
    }
}
