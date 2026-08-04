package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.workflow.dto.WorkflowDefinitionDto;
import com.frezo.common.workflow.dto.WorkflowGraphDto;
import com.frezo.common.workflow.service.WorkflowVisualService;
import com.frezo.common.security.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Visual Workflow API — storage-first (graph + guide trên BE).
 * <p>
 * Không thay thế {@link WorkflowController} ({@code /wf/*} SIMPLE).
 */
@RestController
@RequestMapping("/workflows")
@RequiredArgsConstructor
@Tag(name = "99b. Visual Workflow", description = "Template gallery + designer graph (React Flow)")
public class VisualWorkflowController {

    private final WorkflowVisualService visualService;

    // ---- Templates ----

    @Operation(summary = "Danh sách mẫu hệ thống (gallery)")
    @GetMapping("/templates")
    @CheckPermission(api = "/workflows/templates", action = "VIEW")
    public ApiResponse<?> listTemplates() {
        return ApiResponse.success(visualService.listTemplates());
    }

    @Operation(summary = "Chi tiết mẫu theo code/key (kèm graphJson + guideMarkdown)")
    @GetMapping("/templates/{code}")
    @CheckPermission(api = "/workflows/templates/{code}", action = "VIEW")
    public ApiResponse<?> getTemplate(@PathVariable String code) {
        return ApiResponse.success(visualService.getTemplate(code));
    }

    @Operation(summary = "Clone mẫu → definition VISUAL mới")
    @PostMapping("/templates/{code}/clone")
    @CheckPermission(api = "/workflows/templates/{code}/clone", action = "CREATE")
    public ApiResponse<?> cloneTemplate(@PathVariable String code) {
        return ApiResponse.success(visualService.cloneTemplate(code));
    }

    // ---- Definitions (visual) ----

    @Operation(summary = "GET definition (kèm graphJson)")
    @GetMapping("/definitions/{id}")
    @CheckPermission(api = "/workflows/definitions/{id}", action = "VIEW")
    public ApiResponse<?> getDefinition(@PathVariable String id) {
        return ApiResponse.success(visualService.getDefinitionVisual(id));
    }

    @Operation(summary = "PUT definition (nhận graphJson / guideMarkdown)")
    @PutMapping("/definitions/{id}")
    @CheckPermission(api = "/workflows/definitions/{id}", action = "UPDATE")
    public ApiResponse<?> updateDefinition(@PathVariable String id, @RequestBody WorkflowDefinitionDto dto) {
        return ApiResponse.success(visualService.updateDefinitionVisual(id, dto));
    }

    @Operation(summary = "Validate graph: 1 START, ≥1 END, reachable, DECISION ≥2 edges")
    @PostMapping("/definitions/{id}/validate")
    @CheckPermission(api = "/workflows/definitions/{id}/validate", action = "VIEW")
    public ApiResponse<?> validate(@PathVariable String id) {
        return ApiResponse.success(visualService.validate(id));
    }

    // ---- Alias graph endpoints (FE hiện dùng /workflows/{id}/graph) ----

    @Operation(summary = "GET graph (alias)")
    @GetMapping("/{id}/graph")
    @CheckPermission(api = "/workflows/{id}/graph", action = "VIEW")
    public ApiResponse<?> getGraph(@PathVariable String id) {
        return ApiResponse.success(visualService.getGraph(id));
    }

    @Operation(summary = "PUT graph (alias)")
    @PutMapping("/{id}/graph")
    @CheckPermission(api = "/workflows/{id}/graph", action = "UPDATE")
    public ApiResponse<?> saveGraph(@PathVariable String id, @RequestBody WorkflowGraphDto graph) {
        return ApiResponse.success(visualService.saveGraph(id, graph));
    }
}
