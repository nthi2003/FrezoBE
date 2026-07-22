package com.frezo.accounting.controller;

import com.frezo.accounting.dto.request.AccountingSettingRequest;
import com.frezo.accounting.service.AccountingSettingService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounting/setting")
@RequiredArgsConstructor
@Tag(name = "Accounting - Setting", description = "Cấu hình kế toán (chuẩn TT133/TT99, mapping TK)")
public class AccountingSettingController {

    private final AccountingSettingService svc;

    @GetMapping
    @CheckPermission(api = "/accounting/setting", action = "VIEW")
    public ApiResponse<?> get() {
        return ApiResponse.ok(svc.view());
    }

    @PutMapping
    @CheckPermission(api = "/accounting/setting", action = "UPDATE")
    @Operation(summary = "Cập nhật + tuỳ chọn seed COA")
    public ApiResponse<?> update(@RequestBody @Valid AccountingSettingRequest req) {
        return ApiResponse.ok(svc.update(req));
    }
}
