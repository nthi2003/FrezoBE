package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.PageResponse;
import com.frezo.qlns.dto.request.PayrollPeriodRequest;
import com.frezo.qlns.dto.response.PayrollPeriodResponse;
import com.frezo.qlns.service.PayrollPeriodService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qlns/payroll-period")
@RequiredArgsConstructor
@Tag(name = "Quản lý kỳ lương", description = "API quản lý kỳ lương")
public class PayrollPeriodController {

    private final PayrollPeriodService payrollPeriodService;

    @Operation(summary = "Tạo kỳ lương mới")
    @PostMapping
    public ApiResponse<PayrollPeriodResponse> create(@RequestBody PayrollPeriodRequest request) {
        return ApiResponse.success(payrollPeriodService.create(request));
    }

    @Operation(summary = "Cập nhật kỳ lương")
    @PutMapping("/{id}")
    public ApiResponse<PayrollPeriodResponse> update(@PathVariable String id, @RequestBody PayrollPeriodRequest request) {
        return ApiResponse.success(payrollPeriodService.update(id, request));
    }

    @Operation(summary = "Khóa kỳ lương")
    @PutMapping("/{id}/lock")
    public ApiResponse<PayrollPeriodResponse> lock(@PathVariable String id) {
        return ApiResponse.success(payrollPeriodService.lock(id));
    }

    @Operation(summary = "Mở khóa kỳ lương")
    @PutMapping("/{id}/unlock")
    public ApiResponse<PayrollPeriodResponse> unlock(@PathVariable String id) {
        return ApiResponse.success(payrollPeriodService.unlock(id));
    }

    @Operation(summary = "Đóng kỳ lương")
    @PutMapping("/{id}/close")
    public ApiResponse<PayrollPeriodResponse> close(@PathVariable String id) {
        return ApiResponse.success(payrollPeriodService.close(id));
    }

    @Operation(summary = "Chi tiết kỳ lương")
    @GetMapping("/{id}")
    public ApiResponse<PayrollPeriodResponse> getById(@PathVariable String id) {
        return ApiResponse.success(payrollPeriodService.getById(id));
    }

    @Operation(summary = "Danh sách kỳ lương")
    @GetMapping
    public ApiResponse<PageResponse<PayrollPeriodResponse>> getAll(
            @RequestParam(required = false) String orgId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNumber,
            @RequestParam(defaultValue = "12") Integer pageSize) {
        return ApiResponse.success(payrollPeriodService.getAll(orgId, month, year, status, pageNumber, pageSize));
    }
}
