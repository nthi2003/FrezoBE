package com.frezo.common.controller;

import com.frezo.common.entity.AuditLog;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.common.service.AuditLogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/qtht/audit-log", "/qtht/audit-logs"})
@RequiredArgsConstructor
@Tag(name = "X. Audit Log", description = "API truy vấn lịch sử thay đổi dữ liệu")
public class AuditLogController {

    private final AuditLogQueryService auditLogQueryService;

    @GetMapping("/health")
    public String health() {
        return "AuditLogController is UP";
    }

    @Operation(summary = "Lấy danh sách Audit Log", description = "Lấy lịch sử thay đổi dữ liệu hệ thống")
    @GetMapping
    @CheckPermission(api = "/qtht/audit-log", action = "VIEW")
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(ApiResponse.success(auditLogQueryService.search(page, size, keyword)));
    }
}
