package com.frezo.crm.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.crm.common.LeadStatus;
import com.frezo.crm.dto.LeadRequest;
import com.frezo.crm.service.LeadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/crm/leads")
@RequiredArgsConstructor
@Tag(name = "CRM - Leads")
public class LeadController {

    private final LeadService svc;

    @GetMapping
    public ApiResponse<?> list(@RequestParam(required = false) LeadStatus status,
                               @RequestParam(required = false) String owner) {
        if (status != null) return ApiResponse.ok(svc.byStatus(status));
        if (owner != null) return ApiResponse.ok(svc.byOwner(owner));
        return ApiResponse.ok(svc.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<?> get(@PathVariable String id) { return ApiResponse.ok(svc.get(id)); }

    @PostMapping
    public ApiResponse<?> create(@RequestBody @Valid LeadRequest req) {
        return ApiResponse.created(svc.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody @Valid LeadRequest req) {
        return ApiResponse.ok(svc.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        svc.delete(id);
        return ApiResponse.noContent();
    }

    @PostMapping("/{id}/convert")
    public ApiResponse<?> convert(@PathVariable String id,
                                  @RequestParam(required = false) String pipelineId,
                                  @RequestParam(required = false) String stageId,
                                  @RequestParam(required = false) String customerId,
                                  @RequestParam(required = false) BigDecimal amount) {
        return ApiResponse.ok(java.util.Map.of("dealId",
                svc.convert(id, pipelineId, stageId, customerId, amount)));
    }
}
