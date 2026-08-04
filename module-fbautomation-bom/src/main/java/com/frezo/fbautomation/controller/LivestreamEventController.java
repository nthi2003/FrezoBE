package com.frezo.fbautomation.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.fbautomation.dto.request.LivestreamEventRequest;
import com.frezo.fbautomation.dto.response.LivestreamEventResponse;
import com.frezo.fbautomation.service.LivestreamEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/mkt/live")
@RequiredArgsConstructor
@Tag(name = "MKT · Live", description = "Livestream reminder (standalone)")
public class LivestreamEventController {

    private final LivestreamEventService service;

    @GetMapping("/dashboard")
    @CheckPermission(api = "/mkt/live/dashboard", action = "VIEW")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(service.dashboard());
    }

    @GetMapping
    @CheckPermission(api = "/mkt/live", action = "VIEW")
    public ApiResponse<List<LivestreamEventResponse>> list(
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(service.list(status));
    }

    @GetMapping("/{id}")
    @CheckPermission(api = "/mkt/live/{id}", action = "VIEW")
    public ApiResponse<LivestreamEventResponse> get(@PathVariable String id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @CheckPermission(api = "/mkt/live", action = "CREATE")
    public ApiResponse<LivestreamEventResponse> create(@RequestBody @Valid LivestreamEventRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    @PutMapping("/{id}")
    @CheckPermission(api = "/mkt/live/{id}", action = "UPDATE")
    public ApiResponse<LivestreamEventResponse> update(
            @PathVariable String id,
            @RequestBody @Valid LivestreamEventRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/mkt/live/{id}", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/notify")
    @CheckPermission(api = "/mkt/live/{id}/notify", action = "UPDATE")
    @Operation(summary = "Đánh dấu đã gửi nhắc trước giờ live")
    public ApiResponse<LivestreamEventResponse> markNotified(@PathVariable String id) {
        return ApiResponse.ok(service.markNotified(id));
    }

    @PostMapping("/{id}/status")
    @CheckPermission(api = "/mkt/live/{id}/status", action = "UPDATE")
    @Operation(summary = "Đổi trạng thái SCHEDULED|LIVE|ENDED|CANCELLED")
    public ApiResponse<LivestreamEventResponse> updateStatus(
            @PathVariable String id,
            @RequestParam String status) {
        return ApiResponse.ok(service.updateStatus(id, status));
    }
}
