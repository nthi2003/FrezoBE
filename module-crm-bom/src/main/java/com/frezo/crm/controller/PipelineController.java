package com.frezo.crm.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.crm.dto.PipelineRequest;
import com.frezo.crm.service.PipelineService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/crm/pipelines")
@RequiredArgsConstructor
@Tag(name = "CRM - Pipelines & Stages")
public class PipelineController {

    private final PipelineService svc;

    @GetMapping
    @CheckPermission(api = "/crm/pipelines", action = "VIEW")
    public ApiResponse<?> list() { return ApiResponse.ok(svc.list()); }

    @GetMapping("/{id}")
    @CheckPermission(api = "/crm/pipelines/{id}", action = "VIEW")
    public ApiResponse<?> get(@PathVariable String id) { return ApiResponse.ok(svc.get(id)); }

    @GetMapping("/{id}/stages")
    @CheckPermission(api = "/crm/pipelines/{id}/stages", action = "VIEW")
    public ApiResponse<?> stages(@PathVariable String id) {
        return ApiResponse.ok(svc.stages(id));
    }

    @PostMapping
    @CheckPermission(api = "/crm/pipelines", action = "CREATE")
    public ApiResponse<?> create(@RequestBody @Valid PipelineRequest req) {
        return ApiResponse.created(svc.create(req));
    }

    @PutMapping("/{id}")
    @CheckPermission(api = "/crm/pipelines/{id}", action = "UPDATE")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody @Valid PipelineRequest req) {
        return ApiResponse.ok(svc.update(id, req));
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/crm/pipelines/{id}", action = "DELETE")
    public ApiResponse<?> delete(@PathVariable String id) {
        svc.delete(id);
        return ApiResponse.noContent();
    }

    @PostMapping("/ensure-default")
    @CheckPermission(api = "/crm/pipelines/ensure-default", action = "CREATE")
    public ApiResponse<?> ensureDefault() { return ApiResponse.ok(svc.ensureDefault()); }
}
