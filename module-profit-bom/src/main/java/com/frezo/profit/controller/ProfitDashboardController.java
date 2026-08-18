package com.frezo.profit.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.profit.service.ProfitDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;

@RestController
@RequestMapping("/profit/dashboard")
@RequiredArgsConstructor
@Tag(name = "Profit — Dashboard", description = "Break-even, revenue gap, sales KPI")
public class ProfitDashboardController {

    private final ProfitDashboardService profitDashboardService;

    @GetMapping
    @CheckPermission(api = "/profit/dashboard", action = "VIEW")
    @Operation(summary = "Dashboard hòa vốn", description = "Doanh thu thực tế vs mục tiêu — còn thiếu bao nhiêu")
    public ApiResponse<?> dashboard(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(defaultValue = "false") boolean snapshot) {
        YearMonth ym = resolvePeriod(year, month);
        return ApiResponse.success(profitDashboardService.getDashboard(ym.getYear(), ym.getMonthValue(), snapshot));
    }

    @GetMapping("/sales-kpi")
    @CheckPermission(api = "/profit/dashboard/sales-kpi", action = "VIEW")
    @Operation(summary = "KPI doanh thu theo NVKD")
    public ApiResponse<?> salesKpi(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        YearMonth ym = resolvePeriod(year, month);
        return ApiResponse.success(profitDashboardService.getSalesKpi(ym.getYear(), ym.getMonthValue()));
    }

    private static YearMonth resolvePeriod(Integer year, Integer month) {
        if (year != null && month != null) {
            return YearMonth.of(year, month);
        }
        return YearMonth.now();
    }
}
