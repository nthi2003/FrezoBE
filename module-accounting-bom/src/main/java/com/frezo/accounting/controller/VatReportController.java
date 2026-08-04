package com.frezo.accounting.controller;

import com.frezo.accounting.dto.response.VatReportResponse;
import com.frezo.accounting.service.VatReportService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/accounting/tax")
@RequiredArgsConstructor
@Tag(name = "Accounting — Tax VAT stub")
public class VatReportController {

    private final VatReportService vatReportService;

    @GetMapping("/vat")
    @CheckPermission(api = "/accounting/tax/vat", action = "VIEW")
    public ApiResponse<VatReportResponse> vat(
            @RequestParam int year,
            @RequestParam int month) {
        return ApiResponse.ok(vatReportService.summarize(year, month));
    }
}
