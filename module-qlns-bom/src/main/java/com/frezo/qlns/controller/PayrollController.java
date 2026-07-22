package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qlns.dto.request.PayrollFilter;
import com.frezo.qlns.dto.response.PayrollCalculateAllResponse;
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

    @Operation(
            summary = "Tính lương hàng loạt (POST only)",
            description = "Method = POST (GET cùng path → 405). Query bắt buộc: month (1–12), year (2000–2100). "
                    + "Response: PayrollCalculateAllResponse (successCount, skippedCount, errorCount, "
                    + "totalCandidates, errors[{personId,personName,personCode,reason}], warnings[]). "
                    + "NV thiếu HĐ activated/ACTIVE → skip reason=NO_ACTIVE_CONTRACT (không silent, không 500 cả batch). "
                    + "1 NV lỗi kỹ thuật → vẫn HTTP 200 + errorCount/errors[].")
    @PostMapping("/calculate-all")
    public ApiResponse<PayrollCalculateAllResponse> calculateAll(
            @RequestParam Integer month, @RequestParam Integer year) {
        PayrollCalculateAllResponse summary = payrollService.calculateAllPayroll(month, year);
        // LNK01-02 / LNK02-07: không báo “hoàn tất” khi toàn skip hoặc có lỗi item
        String msg = buildCalculateAllMessage(summary);
        return ApiResponse.success(summary, msg);
    }

    private static String buildCalculateAllMessage(PayrollCalculateAllResponse summary) {
        int success = summary.getSuccessCount() != null ? summary.getSuccessCount() : 0;
        int skipped = summary.getSkippedCount() != null ? summary.getSkippedCount() : 0;
        int errors = summary.getErrorCount() != null ? summary.getErrorCount() : 0;
        if (success == 0 && skipped > 0) {
            return "Không tạo được bảng lương — đã bỏ qua " + skipped + " NV thiếu HĐ đang hiệu lực";
        }
        if (skipped > 0 || errors > 0) {
            return "Tính lương hàng loạt xong (có cảnh báo: bỏ qua " + skipped + ", lỗi " + errors + ")";
        }
        return "Tính lương hàng loạt hoàn tất";
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

    @Operation(
            summary = "Danh sách bảng lương",
            description = "Filter month/year/personId/status. pageNumber omit → default 1 (1-based); "
                    + "pageSize mặc định 10, màn kỳ có thể truyền tới 500. Trả PageResponse (items array, có thể rỗng).")
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
