package com.frezo.common.controller;

import com.frezo.common.entity.AuditLog;
import com.frezo.common.repository.AuditLogRepository;
import com.frezo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/qtht/audit-log")
@RequiredArgsConstructor
@Tag(name = "X. Audit Log", description = "API truy vấn lịch sử thay đổi dữ liệu")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping("/health")
    public String health() {
        return "AuditLogController is UP";
    }

    @Operation(summary = "Lấy danh sách Audit Log", description = "Lấy lịch sử thay đổi dữ liệu hệ thống")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLog>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("timestamp").descending());
        
        Page<AuditLog> result;
        if (keyword != null && !keyword.isEmpty()) {
            result = auditLogRepository.findByUsernameContainingOrActionContaining(keyword, keyword, pageRequest);
        } else {
            result = auditLogRepository.findAll(pageRequest);
        }
        
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
