package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.common.response.FePage;
import com.frezo.warehouse.dto.request.ReorderRuleRequest;
import com.frezo.warehouse.dto.response.ReorderRuleDto;
import com.frezo.warehouse.dto.response.StockAlertDto;
import com.frezo.warehouse.dto.response.WarehouseOptionDto;
import com.frezo.warehouse.job.StockAlertJob;
import com.frezo.warehouse.service.ReorderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/warehouse")
@RequiredArgsConstructor
@Tag(name = "Warehouse — Reorder & Alerts", description = "ReorderRule + StockAlert")
public class ReorderController {

    private final ReorderService reorderService;
    private final StockAlertJob stockAlertJob;

    @GetMapping("/warehouses")
    @CheckPermission(api = "/warehouse/warehouses", action = "VIEW")
    @Operation(summary = "Danh sách kho (option combobox)")
    public ApiResponse<List<WarehouseOptionDto>> listWarehouses() {
        return ApiResponse.ok(reorderService.listWarehouses());
    }

    @GetMapping("/reorder-rules")
    @CheckPermission(api = "/warehouse/reorder-rules", action = "VIEW")
    public ApiResponse<FePage<ReorderRuleDto>> listRules(
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String productId) {
        return ApiResponse.ok(reorderService.listRules(warehouseId, productId));
    }

    @PostMapping("/reorder-rules")
    @CheckPermission(api = "/warehouse/reorder-rules", action = "CREATE")
    public ApiResponse<ReorderRuleDto> create(@RequestBody ReorderRuleRequest req) {
        return ApiResponse.ok(reorderService.create(req));
    }

    @PutMapping("/reorder-rules/{id}")
    @CheckPermission(api = "/warehouse/reorder-rules/{id}", action = "UPDATE")
    public ApiResponse<ReorderRuleDto> update(@PathVariable String id,
                                              @RequestBody ReorderRuleRequest req) {
        return ApiResponse.ok(reorderService.update(id, req));
    }

    @DeleteMapping("/reorder-rules/{id}")
    @CheckPermission(api = "/warehouse/reorder-rules/{id}", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable String id) {
        reorderService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/reorder-rules/import-excel")
    @CheckPermission(api = "/warehouse/reorder-rules/import-excel", action = "UPDATE")
    public ApiResponse<Map<String, Integer>> importExcel(
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ApiResponse.ok(reorderService.importExcel(file));
    }

    @GetMapping("/stock-alerts")
    @CheckPermission(api = "/warehouse/stock-alerts", action = "VIEW")
    public ApiResponse<FePage<StockAlertDto>> listAlerts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String alertType) {
        return ApiResponse.ok(reorderService.listAlerts(status, alertType));
    }

    @PostMapping("/stock-alerts/scan")
    @CheckPermission(api = "/warehouse/stock-alerts/scan", action = "CREATE")
    @Operation(summary = "Chạy scan cảnh báo tồn/cận hạn ngay (DEV/QA — cùng logic cron 06:00)")
    public ApiResponse<Map<String, String>> scanNow() {
        stockAlertJob.runMorningScan();
        return ApiResponse.ok(Map.of(
                "status", "ok",
                "note", "Đã scan LOW_STOCK + EXPIRY_SOON và gửi notification (idempotent trong ngày)"));
    }

    @PostMapping("/stock-alerts/{id}/dismiss")
    @CheckPermission(api = "/warehouse/stock-alerts/{id}/dismiss", action = "CREATE")
    public ApiResponse<StockAlertDto> dismiss(@PathVariable String id) {
        return ApiResponse.ok(reorderService.dismiss(id));
    }
}
