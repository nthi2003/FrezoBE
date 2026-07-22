package com.frezo.accounting.controller;

import com.frezo.accounting.service.FiscalPeriodService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/accounting/periods")
@RequiredArgsConstructor
@Tag(name = "Accounting - Fiscal Period", description = "Năm & kỳ kế toán")
public class FiscalPeriodController {

    private final FiscalPeriodService svc;

    @GetMapping
    @CheckPermission(api = "/accounting/periods", action = "VIEW")
    public ApiResponse<?> list(@RequestParam(required = false) Integer year) {
        int y = year != null ? year : LocalDate.now().getYear();
        return ApiResponse.ok(svc.listByYear(y));
    }

    @PostMapping("/ensure")
    @CheckPermission(api = "/accounting/periods", action = "CREATE")
    @Operation(summary = "Tạo năm tài chính + 12 kỳ nếu chưa có (idempotent)")
    public ApiResponse<?> ensure(@RequestParam int year) {
        return ApiResponse.ok(svc.ensureYear(year));
    }

    @PostMapping("/{id}/close")
    @CheckPermission(api = "/accounting/periods", action = "UPDATE")
    public ApiResponse<?> close(@PathVariable String id) {
        return ApiResponse.ok(svc.closePeriod(id));
    }

    @PostMapping("/{id}/reopen")
    @CheckPermission(api = "/accounting/periods", action = "UPDATE")
    public ApiResponse<?> reopen(@PathVariable String id) {
        return ApiResponse.ok(svc.reopenPeriod(id));
    }
}
