package com.frezo.fbautomation.controller;

import com.frezo.fbautomation.dto.response.LeadImportBatchResponse;
import com.frezo.fbautomation.service.LeadImportService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * LeadImportController — upload CSV/Excel để tạo lead hàng loạt.
 * <p>
 * Endpoints:
 * - POST /mkt/leads/import              — upload file, tự tạo batch + insert
 * - POST /mkt/leads/import/preview      — preview 20 dòng đầu (check format trước khi import)
 * - GET  /mkt/leads/import/history      — lịch sử batch đã upload
 * - DELETE /mkt/leads/import/{batchId}  — rollback batch (soft-delete lead con)
 */
@RestController
@RequestMapping("/mkt/leads/import")
@RequiredArgsConstructor
@Tag(name = "MKT · Lead Import", description = "Nhập lead hàng loạt từ CSV/Excel")
public class LeadImportController {

    private final LeadImportService importService;

    @Operation(summary = "Upload file CSV/Excel để tạo lead hàng loạt")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CheckPermission(api = "/mkt/leads/import", action = "UPDATE")
    public ApiResponse<LeadImportBatchResponse> importFile(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "true") boolean dedupe) {
        return ApiResponse.ok(importService.importLeads(file, source, dedupe));
    }

    @Operation(summary = "Preview 20 dòng đầu để check format")
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @CheckPermission(api = "/mkt/leads/import/preview", action = "CREATE")
    public ApiResponse<List<List<String>>> preview(@RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(importService.preview(file));
    }

    @Operation(summary = "Lịch sử các batch đã upload")
    @GetMapping("/history")
    @CheckPermission(api = "/mkt/leads/import/history", action = "VIEW")
    public ApiResponse<List<LeadImportBatchResponse>> history() {
        return ApiResponse.ok(importService.history());
    }

    @Operation(summary = "Rollback batch — soft-delete tất cả lead con")
    @DeleteMapping("/{batchId}")
    @CheckPermission(api = "/mkt/leads/import/{batchId}", action = "DELETE")
    public ApiResponse<Void> rollback(@PathVariable String batchId) {
        importService.rollback(batchId);
        return ApiResponse.ok();
    }
}
