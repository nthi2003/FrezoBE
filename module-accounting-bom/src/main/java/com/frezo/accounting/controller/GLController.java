package com.frezo.accounting.controller;

import com.frezo.accounting.service.GLService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/accounting/gl")
@RequiredArgsConstructor
@Tag(name = "Accounting - General Ledger", description = "Sổ cái + Bảng cân đối phát sinh")
public class GLController {

    private final GLService svc;

    @GetMapping("/ledger")
    @CheckPermission(api = "/accounting/gl", action = "VIEW")
    @Operation(summary = "Sổ cái theo tài khoản trong khoảng thời gian")
    public ApiResponse<?> ledger(@RequestParam String accountCode,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(svc.getLedger(accountCode, from, to));
    }

    @GetMapping("/trial-balance")
    @CheckPermission(api = "/accounting/gl", action = "VIEW")
    @Operation(summary = "Bảng cân đối phát sinh")
    public ApiResponse<?> trialBalance(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(svc.trialBalance(from, to));
    }
}
