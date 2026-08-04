package com.frezo.accounting.controller;

import com.frezo.accounting.common.AccountingStandard;
import com.frezo.accounting.dto.request.AccountRequest;
import com.frezo.accounting.service.AccountService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accounting/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounting - COA", description = "Chart of Accounts (Hệ thống tài khoản kế toán)")
public class AccountController {

    private final AccountService svc;

    @GetMapping
    @CheckPermission(api = "/accounting/accounts", action = "VIEW")
    @Operation(summary = "Danh sách toàn bộ tài khoản")
    public ApiResponse<?> list(@RequestParam(required = false) AccountingStandard standard) {
        return ApiResponse.ok(standard != null ? svc.listByStandard(standard) : svc.listAll());
    }

    @GetMapping("/{id}")
    @CheckPermission(api = "/accounting/accounts/{id}", action = "VIEW")
    public ApiResponse<?> get(@PathVariable String id) {
        return ApiResponse.ok(svc.getById(id));
    }

    @PostMapping
    @CheckPermission(api = "/accounting/accounts", action = "CREATE")
    @Operation(summary = "Tạo mới tài khoản (mở rộng COA)")
    public ApiResponse<?> create(@RequestBody @Valid AccountRequest req) {
        return ApiResponse.created(svc.create(req));
    }

    @PutMapping("/{id}")
    @CheckPermission(api = "/accounting/accounts/{id}", action = "UPDATE")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody @Valid AccountRequest req) {
        return ApiResponse.ok(svc.update(id, req));
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/accounting/accounts/{id}", action = "DELETE")
    public ApiResponse<?> delete(@PathVariable String id) {
        svc.delete(id);
        return ApiResponse.noContent();
    }

    @PostMapping("/seed")
    @CheckPermission(api = "/accounting/accounts/seed", action = "CREATE")
    @Operation(summary = "Seed COA theo Thông tư",
            description = "Idempotent — chỉ thêm TK chưa có. Trả về số TK vừa được tạo.")
    public ApiResponse<?> seed(@RequestParam AccountingStandard standard) {
        return ApiResponse.ok(java.util.Map.of("created", svc.seedChartOfAccounts(standard)));
    }
}
