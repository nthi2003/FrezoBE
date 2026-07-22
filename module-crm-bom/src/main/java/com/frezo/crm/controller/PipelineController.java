package com.frezo.crm.controller;

import com.frezo.common.response.ApiResponse;
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
    public ApiResponse<?> list() { return ApiResponse.ok(svc.list()); }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable String id) { return ApiResponse.ok(svc.get(id)); }

    @GetMapping("/{id}/stages")
    public ApiResponse<?> stages(@PathVariable String id) {
        return ApiResponse.ok(svc.stages(id));
    }

    @PostMapping
    public ApiResponse<?> create(@RequestBody @Valid PipelineRequest req) {
        return ApiResponse.created(svc.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody @Valid PipelineRequest req) {
        return ApiResponse.ok(svc.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        svc.delete(id);
        return ApiResponse.noContent();
    }

    @PostMapping("/ensure-default")
    public ApiResponse<?> ensureDefault() { return ApiResponse.ok(svc.ensureDefault()); }
}
