package com.frezo.profit.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.profit.dto.request.ProfitCalculateRequest;
import com.frezo.profit.dto.request.ProfitSettingRequest;
import com.frezo.profit.service.ProfitCalculatorService;
import com.frezo.profit.service.ProfitSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profit/settings")
@RequiredArgsConstructor
@Tag(name = "Profit — Settings", description = "Cấu hình chi phí & biên lợi nhuận")
public class ProfitSettingController {

    private final ProfitSettingService profitSettingService;

    @GetMapping
    @CheckPermission(api = "/profit/settings", action = "VIEW")
    public ApiResponse<?> list(@RequestParam(required = false) String scope) {
        return ApiResponse.success(profitSettingService.list(scope));
    }

    @GetMapping("/{id}")
    @CheckPermission(api = "/profit/settings/{id}", action = "VIEW")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(profitSettingService.getById(id));
    }

    @GetMapping("/resolve/{productId}")
    @CheckPermission(api = "/profit/settings/resolve/{productId}", action = "VIEW")
    @Operation(summary = "Cấu hình áp dụng cho sản phẩm (PRODUCT > CATEGORY > GLOBAL)")
    public ApiResponse<?> resolveForProduct(@PathVariable String productId) {
        return ApiResponse.success(profitSettingService.resolveForProduct(productId));
    }

    @PostMapping
    @CheckPermission(api = "/profit/settings", action = "CREATE")
    public ApiResponse<?> create(@Valid @RequestBody ProfitSettingRequest req) {
        return ApiResponse.success(profitSettingService.create(req));
    }

    @PutMapping("/{id}")
    @CheckPermission(api = "/profit/settings/{id}", action = "UPDATE")
    public ApiResponse<?> update(@PathVariable String id, @Valid @RequestBody ProfitSettingRequest req) {
        return ApiResponse.success(profitSettingService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/profit/settings/{id}", action = "DELETE")
    public ApiResponse<?> delete(@PathVariable String id) {
        profitSettingService.delete(id);
        return ApiResponse.success("Đã xóa cấu hình");
    }
}
