package com.frezo.accounting.controller;

import com.frezo.accounting.service.AccountingDashboardService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounting/dashboard")
@RequiredArgsConstructor
@Tag(name = "Accounting - Dashboard", description = "Tổng quan kế toán")
public class AccountingDashboardController {

    private final AccountingDashboardService dashboardService;

    @GetMapping("/summary")
    @CheckPermission(api = "/accounting/gl/trial-balance", action = "VIEW")
    @Operation(summary = "Tổng quan KPI, biểu đồ và giao dịch gần đây")
    public ApiResponse<?> summary(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month) {
        int y = year > 0 ? year : java.time.LocalDate.now().getYear();
        int m = month > 0 ? month : java.time.LocalDate.now().getMonthValue();
        return ApiResponse.ok(dashboardService.getSummary(y, m));
    }
}
