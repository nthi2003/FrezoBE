package com.frezo.fbautomation.controller;

import com.frezo.fbautomation.dto.response.FacebookLeadResponse;
import com.frezo.fbautomation.service.FacebookLeadService;
import com.frezo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/fb/leads")
@RequiredArgsConstructor
@Tag(name = "Facebook Leads", description = "Khách hàng tiềm năng từ Facebook Groups")
public class FacebookLeadController {

    private final FacebookLeadService leadService;

    @Operation(summary = "Danh sách leads (lọc theo status + source)")
    @GetMapping
    public ApiResponse<List<FacebookLeadResponse>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String source) {
        return ApiResponse.ok(leadService.getAll(status, source));
    }

    @Operation(summary = "Assign lead cho nhân viên CSKH")
    @PostMapping("/{id}/assign")
    public ApiResponse<FacebookLeadResponse> assign(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (username == null || username.isBlank()) {
            return ApiResponse.error("username không được trống");
        }
        return ApiResponse.ok(leadService.assign(id, username));
    }

    @Operation(summary = "Chi tiết lead")
    @GetMapping("/{id}")
    public ApiResponse<FacebookLeadResponse> getById(@PathVariable String id) {
        return ApiResponse.ok(leadService.getById(id));
    }

    @Operation(summary = "Xóa lead")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        leadService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "Import lead vào danh sách khách hàng")
    @PostMapping("/{id}/import")
    public ApiResponse<String> importToCustomer(@PathVariable String id) {
        String customerId = leadService.importToCustomer(id);
        return ApiResponse.ok("Đã import thành công, customerId: " + customerId);
    }

    @Operation(summary = "Import nhiều leads vào danh sách khách hàng")
    @PostMapping("/import-batch")
    public ApiResponse<String> importBatch(@RequestBody Map<String, List<String>> body) {
        List<String> ids = body.get("ids");
        if (ids == null || ids.isEmpty()) {
            return ApiResponse.error("Danh sách IDs không được trống");
        }
        int count = leadService.importAllToCustomer(ids);
        return ApiResponse.ok("Đã import thành công " + count + "/" + ids.size() + " khách hàng");
    }
}
