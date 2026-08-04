package com.frezo.crm.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.crm.entity.CommissionEntry;
import com.frezo.crm.entity.CommissionRule;
import com.frezo.crm.service.CommissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/crm/commissions")
@RequiredArgsConstructor
@Tag(name = "CRM - Hoa hồng sale")
public class CommissionController {

    private final CommissionService commissionService;

    @Operation(summary = "Dashboard hoa hồng theo sale (số đơn + tiền HH)")
    @GetMapping("/dashboard")
    @CheckPermission(api = "/crm/commissions/dashboard", action = "VIEW")
    public ApiResponse<?> dashboard() {
        return ApiResponse.ok(commissionService.dashboard());
    }

    @Operation(summary = "Danh sách cấu hình mức hoa hồng")
    @GetMapping("/rules")
    @CheckPermission(api = "/crm/commissions/rules", action = "VIEW")
    public ApiResponse<?> listRules() {
        return ApiResponse.ok(commissionService.listRules());
    }

    @Operation(summary = "Tạo/cập nhật mức hoa hồng cho sale (username=* = mặc định)")
    @PostMapping("/rules")
    @CheckPermission(api = "/crm/commissions/rules", action = "CREATE")
    public ApiResponse<?> upsertRule(@RequestBody RuleRequest req) {
        CommissionRule saved = commissionService.upsertRule(
                req.getSalespersonUsername(),
                req.getRatePercent(),
                req.getActive(),
                req.getNote());
        return ApiResponse.ok(saved);
    }

    @Operation(summary = "Xoá cấu hình sale (không xoá mức *)")
    @DeleteMapping("/rules/{id}")
    @CheckPermission(api = "/crm/commissions/rules/{id}", action = "DELETE")
    public ApiResponse<?> deleteRule(@PathVariable String id) {
        commissionService.deleteRule(id);
        return ApiResponse.noContent();
    }

    @Operation(summary = "Resolve % hoa hồng cho 1 sale")
    @GetMapping("/resolve-rate")
    @CheckPermission(api = "/crm/commissions/resolve-rate", action = "VIEW")
    public ApiResponse<?> resolveRate(@RequestParam(required = false) String salespersonUsername) {
        BigDecimal rate = commissionService.resolveRatePercent(salespersonUsername);
        return ApiResponse.ok(Map.of(
                "salespersonUsername", salespersonUsername != null ? salespersonUsername : "",
                "ratePercent", rate));
    }

    @Operation(summary = "Danh sách bản ghi hoa hồng đã phát sinh")
    @GetMapping("/entries")
    @CheckPermission(api = "/crm/commissions/entries", action = "VIEW")
    public ApiResponse<?> listEntries(@RequestParam(required = false) String salespersonUsername) {
        return ApiResponse.ok(commissionService.listEntries(salespersonUsername));
    }

    @PatchMapping("/entries/{id}/approve")
    @CheckPermission(api = "/crm/commissions/entries/{id}/approve", action = "UPDATE")
    public ApiResponse<?> approve(@PathVariable String id) {
        return ApiResponse.ok(commissionService.approve(id));
    }

    @PatchMapping("/entries/{id}/mark-paid")
    @CheckPermission(api = "/crm/commissions/entries/{id}/mark-paid", action = "UPDATE")
    public ApiResponse<?> markPaid(@PathVariable String id) {
        return ApiResponse.ok(commissionService.markPaid(id));
    }

    @PatchMapping("/entries/{id}/void")
    @CheckPermission(api = "/crm/commissions/entries/{id}/void", action = "UPDATE")
    public ApiResponse<?> voidEntry(@PathVariable String id) {
        return ApiResponse.ok(commissionService.voidEntry(id));
    }

    @Data
    public static class RuleRequest {
        private String salespersonUsername;
        private BigDecimal ratePercent;
        private Boolean active;
        private String note;
    }
}
