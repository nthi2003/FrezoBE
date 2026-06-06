package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qlns.dto.request.PayrollFilter;
import com.frezo.qlns.dto.response.PayrollResponse;
import com.frezo.qlns.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qlns/payroll")
@RequiredArgsConstructor
@Tag(name = "Quản lý lương", description = "API quản lý lương nhân viên")
public class PayrollController {

    private final PayrollService payrollService;

    @Operation(summary = "Tính lương hàng tháng cho 1 nhân viên", description = "Tính toán lương dựa trên chấm công và nghỉ phép")
    @PostMapping("/calculate/{personId}")
    public ApiResponse<PayrollResponse> calculate(@PathVariable String personId,
                                                  @RequestParam Integer month,
                                                  @RequestParam Integer year) {
        return ApiResponse.success(payrollService.calculateMonthlyPayroll(personId, month, year));
    }

    @Operation(summary = "Tính lương hàng tháng tự động cho tất cả nhân viên")
    @PostMapping("/calculate-all")
    public ApiResponse<Void> calculateAll(@RequestParam Integer month,
                                          @RequestParam Integer year) {
        payrollService.calculateAllPayroll(month, year);
        return ApiResponse.success(null, "Tính lương hàng loạt thành công");
    }

    @Operation(summary = "Cập nhật thưởng/khấu trừ")
    @PutMapping("/{id}/bonus")
    public ApiResponse<PayrollResponse> updateBonus(@PathVariable String id,
                                                    @RequestParam Double bonus,
                                                    @RequestParam Double deduction,
                                                    @RequestParam(required = false) String note) {
        return ApiResponse.success(payrollService.updateBonus(id, bonus, deduction, note));
    }

    @Operation(summary = "Xác nhận bảng lương")
    @PutMapping("/{id}/confirm")
    public ApiResponse<PayrollResponse> confirm(@PathVariable String id) {
        return ApiResponse.success(payrollService.confirm(id));
    }

    @Operation(summary = "Thanh toán bảng lương")
    @PutMapping("/{id}/pay")
    public ApiResponse<PayrollResponse> pay(@PathVariable String id) {
        return ApiResponse.success(payrollService.pay(id));
    }

    @Operation(summary = "Lấy danh sách bảng lương")
    @GetMapping
    public ApiResponse<?> getAll(@ModelAttribute PayrollFilter filter) {
        return ApiResponse.success(payrollService.getAll(filter));
    }

    @Operation(summary = "Chi tiết bảng lương")
    @GetMapping("/{id}")
    public ApiResponse<PayrollResponse> getById(@PathVariable String id) {
        return ApiResponse.success(payrollService.getById(id));
    }
}
