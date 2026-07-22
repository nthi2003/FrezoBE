package com.frezo.qlns.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qlns.service.PayrollGLPostingService;
import com.frezo.qlns.service.PayslipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Payslip-focused endpoints cho Mobile (Employee Self-Service).
 * Tách khỏi {@code PayrollController} (HR-focused) để giữ contract rõ ràng.
 */
@RestController
@RequestMapping("/qlns/payslip")
@RequiredArgsConstructor
@Tag(name = "Payslip - Mobile", description = "Payslip cho Mobile: full detail, YTD, formulas, confirm")
public class PayslipController {

    private final PayslipService payslipService;
    private final PayrollGLPostingService glPostingService;

    @Operation(summary = "Payslip đầy đủ (Earnings/Deductions/Attendance breakdown) cho Mobile")
    @GetMapping("/{payrollId}")
    public ApiResponse<?> getPayslip(@PathVariable String payrollId) {
        return ApiResponse.ok(payslipService.getPayslip(payrollId));
    }

    @Operation(summary = "YTD summary + 12 tháng để vẽ chart")
    @GetMapping("/ytd")
    public ApiResponse<?> ytd(@RequestParam String personId,
                              @RequestParam Integer year) {
        return ApiResponse.ok(payslipService.getYtd(personId, year));
    }

    @Operation(summary = "Static formulas — nút 'Giải thích công thức' trên Mobile")
    @GetMapping("/formulas")
    public ApiResponse<?> formulas() {
        return ApiResponse.ok(payslipService.getFormulas());
    }

    @Operation(summary = "Nhân viên confirm đã nhận payslip (chữ ký điện tử nhẹ)")
    @PostMapping("/{payrollId}/confirm")
    public ApiResponse<?> confirm(@PathVariable String payrollId,
                                  @RequestParam(required = false) String note,
                                  @RequestHeader(value = "User-Agent", required = false) String userAgent,
                                  HttpServletRequest req) {
        String ip = extractClientIp(req);
        return ApiResponse.ok(payslipService.confirmReceived(payrollId, note, ip, userAgent));
    }

    // ---------- HR endpoints: post payroll → GL ----------

    @Operation(summary = "Hạch toán bảng lương kỳ (tạo bút toán aggregate trong GL)")
    @PostMapping("/period/{year}/{month}/post-to-gl")
    public ApiResponse<?> postToGl(@PathVariable Integer year,
                                   @PathVariable Integer month) {
        String journalId = glPostingService.postPeriod(month, year);
        return ApiResponse.ok(java.util.Map.of("journalEntryId", journalId));
    }

    @Operation(summary = "Đảo bút toán payroll của kỳ (Reversal)")
    @PostMapping("/period/{year}/{month}/reverse-gl")
    public ApiResponse<?> reverseGl(@PathVariable Integer year,
                                    @PathVariable Integer month,
                                    @RequestParam(required = false) String reason) {
        String reversedId = glPostingService.reversePeriod(month, year, reason);
        return ApiResponse.ok(java.util.Map.of("reversalJournalId", reversedId));
    }

    private static String extractClientIp(HttpServletRequest req) {
        String[] headers = {"X-Forwarded-For", "X-Real-IP", "CF-Connecting-IP"};
        for (String h : headers) {
            String v = req.getHeader(h);
            if (v != null && !v.isBlank() && !"unknown".equalsIgnoreCase(v)) {
                return v.split(",")[0].trim();
            }
        }
        return req.getRemoteAddr();
    }
}
