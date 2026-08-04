package com.frezo.crm.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.crm.common.DealStatus;
import com.frezo.crm.dto.DealRequest;
import com.frezo.crm.service.DealService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/crm/deals")
@RequiredArgsConstructor
@Tag(name = "CRM - Deals (Kanban)")
public class DealController {

    private final DealService svc;

    @GetMapping
    @CheckPermission(api = "/crm/deals", action = "VIEW")
    public ApiResponse<?> list(@RequestParam(required = false) String pipelineId,
                               @RequestParam(required = false) DealStatus status,
                               @RequestParam(required = false) String owner,
                               @RequestParam(required = false) String customerId) {
        if (pipelineId != null) return ApiResponse.ok(svc.byPipeline(pipelineId));
        if (status != null) return ApiResponse.ok(svc.byStatus(status));
        if (owner != null) return ApiResponse.ok(svc.byOwner(owner));
        if (customerId != null) return ApiResponse.ok(svc.byCustomer(customerId));
        return ApiResponse.ok(java.util.List.of());
    }

    @GetMapping("/{id}")
    @CheckPermission(api = "/crm/deals/{id}", action = "VIEW")
    public ApiResponse<?> get(@PathVariable String id) { return ApiResponse.ok(svc.get(id)); }

    @PostMapping
    @CheckPermission(api = "/crm/deals", action = "CREATE")
    public ApiResponse<?> create(@RequestBody @Valid DealRequest req) {
        return ApiResponse.created(svc.create(req));
    }

    @PutMapping("/{id}")
    @CheckPermission(api = "/crm/deals/{id}", action = "UPDATE")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody @Valid DealRequest req) {
        return ApiResponse.ok(svc.update(id, req));
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/crm/deals/{id}", action = "DELETE")
    public ApiResponse<?> delete(@PathVariable String id) {
        svc.delete(id);
        return ApiResponse.noContent();
    }

    @PatchMapping("/{id}/move-stage")
    @CheckPermission(api = "/crm/deals/{id}/move-stage", action = "UPDATE")
    public ApiResponse<?> moveStage(@PathVariable String id, @RequestParam String toStageId) {
        return ApiResponse.ok(svc.moveStage(id, toStageId));
    }

    @PatchMapping("/{id}/won")
    @CheckPermission(api = "/crm/deals/{id}/won", action = "UPDATE")
    public ApiResponse<?> won(@PathVariable String id) { return ApiResponse.ok(svc.markWon(id)); }

    @PatchMapping("/{id}/lost")
    @CheckPermission(api = "/crm/deals/{id}/lost", action = "UPDATE")
    public ApiResponse<?> lost(@PathVariable String id, @RequestParam(required = false) String reason) {
        return ApiResponse.ok(svc.markLost(id, reason));
    }
}
