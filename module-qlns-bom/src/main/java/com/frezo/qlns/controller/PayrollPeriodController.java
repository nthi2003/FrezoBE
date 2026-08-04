package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
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
    @CheckPermission(api = "/qlns/payroll-period", action = "CREATE")
    public ApiResponse<PayrollPeriodResponse> create(@RequestBody PayrollPeriodRequest request) {
        return ApiResponse.success(payrollPeriodService.create(request));
    }

    @Operation(summary = "Cập nhật kỳ lương")
    @PutMapping("/{id}")
    @CheckPermission(api = "/qlns/payroll-period/{id}", action = "UPDATE")
    public ApiResponse<PayrollPeriodResponse> update(@PathVariable String id, @RequestBody PayrollPeriodRequest request) {
        return ApiResponse.success(payrollPeriodService.update(id, request));
    }

    @Operation(summary = "Khóa kỳ lương → Approval PAYROLL_PERIOD")
    @PutMapping("/{id}/lock")
    @CheckPermission(api = "/qlns/payroll-period/{id}/lock", action = "UPDATE")
    public ApiResponse<PayrollPeriodResponse> lock(@PathVariable String id) {
        return ApiResponse.success(payrollPeriodService.lock(id));
    }

    @Operation(summary = "Khóa kỳ lương (POST alias)")
    @PostMapping("/{id}/lock")
    @CheckPermission(api = "/qlns/payroll-period/{id}/lock", action = "UPDATE")
    public ApiResponse<PayrollPeriodResponse> lockPost(@PathVariable String id) {
        return ApiResponse.success(payrollPeriodService.lock(id));
    }

    @Operation(summary = "Mở khóa kỳ lương")
    @PutMapping("/{id}/unlock")
    @CheckPermission(api = "/qlns/payroll-period/{id}/unlock", action = "UPDATE")
    public ApiResponse<PayrollPeriodResponse> unlock(@PathVariable String id) {
        return ApiResponse.success(payrollPeriodService.unlock(id));
    }

    @Operation(summary = "Đóng kỳ lương")
    @PutMapping("/{id}/close")
    @CheckPermission(api = "/qlns/payroll-period/{id}/close", action = "UPDATE")
    public ApiResponse<PayrollPeriodResponse> close(@PathVariable String id) {
        return ApiResponse.success(payrollPeriodService.close(id));
    }

    @Operation(summary = "Chi tiết kỳ lương")
    @GetMapping("/{id}")
    @CheckPermission(api = "/qlns/payroll-period/{id}", action = "VIEW")
    public ApiResponse<PayrollPeriodResponse> getById(@PathVariable String id) {
        return ApiResponse.success(payrollPeriodService.getById(id));
    }

    @Operation(summary = "Danh sách kỳ lương")
    @GetMapping
    @CheckPermission(api = "/qlns/payroll-period", action = "VIEW")
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
