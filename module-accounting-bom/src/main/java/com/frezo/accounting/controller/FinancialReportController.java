package com.frezo.accounting.controller;

import com.frezo.accounting.dto.response.BalanceSheetResponse;
import com.frezo.accounting.dto.response.IncomeStatementResponse;
import com.frezo.accounting.service.FinancialReportService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/accounting/reports")
@RequiredArgsConstructor
@Tag(name = "Accounting — Reports", description = "BCTC stub TT133")
public class FinancialReportController {

    private final FinancialReportService financialReportService;

    @GetMapping("/balance-sheet")
    @CheckPermission(api = "/accounting/reports", action = "VIEW")
    @Operation(summary = "Bảng cân đối kế toán (stub từ AccountType + trial-balance)")
    public ApiResponse<BalanceSheetResponse> balanceSheet(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(financialReportService.balanceSheet(from, to));
    }

    @GetMapping("/income-statement")
    @CheckPermission(api = "/accounting/reports", action = "VIEW")
    @Operation(summary = "Báo cáo KQKD (stub từ REVENUE/EXPENSE)")
    public ApiResponse<IncomeStatementResponse> incomeStatement(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(financialReportService.incomeStatement(from, to));
    }
}
