package com.frezo.qlns.controller;

import com.frezo.common.helper.SystemUtils;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.qlns.dto.request.RegularizationAddRequest;
import com.frezo.qlns.service.RegularizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/qlns/attendance-regularization")
@RequiredArgsConstructor
@Tag(name = "Chấm công — Giải trình", description = "Đơn giải trình chấm công (quên/lỗi) — Workflow 1 tầng")
public class RegularizationController {

    private final RegularizationService service;

    @Operation(summary = "Tạo đơn giải trình chấm công")
    @PostMapping
    @CheckPermission(api = "/qlns/attendance-regularization", action = "CREATE")
    public ApiResponse<?> create(@RequestBody RegularizationAddRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @Operation(summary = "Đơn giải trình của tôi (theo personId)")
    @GetMapping("/my/{personId}")
    @CheckPermission(api = "/qlns/attendance-regularization/my/{personId}", action = "VIEW")
    public ApiResponse<?> my(@PathVariable String personId) {
        return ApiResponse.success(service.myRequests(personId));
    }

    @Operation(summary = "Đơn cần tôi duyệt (manager)")
    @GetMapping("/pending")
    @CheckPermission(api = "/qlns/attendance-regularization/pending", action = "VIEW")
    public ApiResponse<?> pending() {
        String me = SystemUtils.getCurrentUsername();
        return ApiResponse.success(service.pendingForManager(me));
    }

    @Operation(summary = "Duyệt đơn giải trình")
    @PutMapping("/{id}/approve")
    @CheckPermission(api = "/qlns/attendance-regularization/{id}/approve", action = "UPDATE")
    public ApiResponse<?> approve(@PathVariable String id) {
        return ApiResponse.success(service.approve(id));
    }

    @Operation(summary = "Từ chối đơn giải trình")
    @PutMapping("/{id}/reject")
    @CheckPermission(api = "/qlns/attendance-regularization/{id}/reject", action = "UPDATE")
    public ApiResponse<?> reject(@PathVariable String id, @RequestBody Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        return ApiResponse.success(service.reject(id, reason));
    }
}
