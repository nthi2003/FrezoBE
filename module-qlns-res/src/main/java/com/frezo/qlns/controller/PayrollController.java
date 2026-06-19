package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qlns.dto.request.PayrollFilter;
import com.frezo.qlns.dto.response.PayrollDetailResponse;
import com.frezo.qlns.dto.response.PayrollResponse;
import com.frezo.qlns.service.PayslipExportService;
import com.frezo.qlns.service.PayrollService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/qlns/payroll")
@RequiredArgsConstructor
@Tag(name = "Quản lý lương", description = "API quản lý lương nhân viên")
public class PayrollController {

    private final PayrollService payrollService;
    private final PayslipExportService payslipExportService;

    @Operation(summary = "Tính lương hàng tháng cho 1 nhân viên")
    @PostMapping("/calculate/{personId}")
    public ApiResponse<PayrollResponse> calculate(@PathVariable String personId,
                                                  @RequestParam Integer month,
                                                  @RequestParam Integer year) {
        return ApiResponse.success(payrollService.calculateMonthlyPayroll(personId, month, year));
    }

    @Operation(summary = "Tính lương hàng loạt cho tất cả nhân viên")
    @PostMapping("/calculate-all")
    public ApiResponse<Void> calculateAll(@RequestParam Integer month, @RequestParam Integer year) {
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

    @Operation(summary = "Xóa bảng lương (soft delete)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        payrollService.deletePayroll(id);
        return ApiResponse.success(null, "Xóa thành công");
    }

    @Operation(summary = "Chi tiết bảng lương (các khoản thu nhập/khấu trừ)")
    @GetMapping("/{id}/details")
    public ApiResponse<List<PayrollDetailResponse>> getDetails(@PathVariable String id) {
        return ApiResponse.success(payrollService.getPayrollDetails(id));
    }

    @Operation(summary = "Danh sách bảng lương")
    @GetMapping
    public ApiResponse<?> getAll(@ModelAttribute PayrollFilter filter) {
        return ApiResponse.success(payrollService.getAll(filter));
    }

    @Operation(summary = "Chi tiết bảng lương theo ID")
    @GetMapping("/{id}")
    public ApiResponse<PayrollResponse> getById(@PathVariable String id) {
        return ApiResponse.success(payrollService.getById(id));
    }

    @Operation(summary = "Xuất phiếu lương (text)")
    @GetMapping("/{id}/export/payslip")
    public ResponseEntity<byte[]> exportPayslip(@PathVariable String id) {
        byte[] data = payslipExportService.exportPayslip(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payslip_" + id + ".txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(data);
    }

    @Operation(summary = "Xuất file chi lương ngân hàng (text)")
    @PostMapping("/export/bank-payment")
    public ResponseEntity<byte[]> exportBankPayment(@RequestBody List<String> payrollIds) {
        byte[] data = payslipExportService.exportBankPayment(payrollIds);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bank_payment.txt")
                .contentType(MediaType.TEXT_PLAIN)
                .body(data);
    }
}
