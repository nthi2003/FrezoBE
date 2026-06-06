package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qtht.dto.request.ApilogFilter;
import com.frezo.qtht.service.ApiLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frezo.qtht.dto.response.ApiLogStatsResponse;

import java.util.Map;

@RestController
@RequestMapping("/apilogs")
@RequiredArgsConstructor
@Tag(name = "ApiLogController", description = "Quản lý API Logs")
public class ApiLogController {

    private final ApiLogService apiLogService;
    private final com.frezo.qtht.mapper.ApiLogMapper apiLogMapper;

    @GetMapping
    @Operation(summary = "Lấy danh sách API logs")
    public ApiResponse<Map<String, Object>> getAllLogs(ApilogFilter filter) {
        return ApiResponse.success(apiLogService.all(filter));
    }

    @GetMapping("/stats")
    @Operation(summary = "Lấy thống kê API logs")
    public ApiResponse<ApiLogStatsResponse> getStats(ApilogFilter filter) {
        return ApiResponse.success(apiLogService.getStats(filter));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy chi tiết API log")
    public ApiResponse<?> getDetail(@PathVariable String id) {
        return ApiResponse.success(apiLogMapper.toResponse(apiLogService.findById(id)));
    }

    @DeleteMapping("/bulk/{days}")
    @Operation(summary = "Xóa API logs theo số ngày")
    public ApiResponse<String> deleteByDays(@PathVariable int days) {
        apiLogService.deleteLogs(days);
        return ApiResponse.success("Xóa thành công log của " + days + " ngày gần đây.");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Xóa một API log")
    public ApiResponse<String> delete(@PathVariable String id) {
        apiLogService.delete(id);
        return ApiResponse.success("Xóa log thành công.");
    }
}
