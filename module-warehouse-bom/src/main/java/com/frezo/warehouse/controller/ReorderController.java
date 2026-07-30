package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.FePage;
import com.frezo.warehouse.dto.request.ReorderRuleRequest;
import com.frezo.warehouse.dto.response.ReorderRuleDto;
import com.frezo.warehouse.dto.response.StockAlertDto;
import com.frezo.warehouse.dto.response.WarehouseOptionDto;
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

    @GetMapping("/warehouses")
    @Operation(summary = "Danh sách kho (option combobox)")
    public ApiResponse<List<WarehouseOptionDto>> listWarehouses() {
        return ApiResponse.ok(reorderService.listWarehouses());
    }

    @GetMapping("/reorder-rules")
    public ApiResponse<FePage<ReorderRuleDto>> listRules(
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String productId) {
        return ApiResponse.ok(reorderService.listRules(warehouseId, productId));
    }

    @PostMapping("/reorder-rules")
    public ApiResponse<ReorderRuleDto> create(@RequestBody ReorderRuleRequest req) {
        return ApiResponse.ok(reorderService.create(req));
    }

    @PutMapping("/reorder-rules/{id}")
    public ApiResponse<ReorderRuleDto> update(@PathVariable String id,
                                              @RequestBody ReorderRuleRequest req) {
        return ApiResponse.ok(reorderService.update(id, req));
    }

    @DeleteMapping("/reorder-rules/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        reorderService.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/reorder-rules/import-excel")
    public ApiResponse<Map<String, Integer>> importExcel(
            @RequestPart(value = "file", required = false) MultipartFile file) {
        return ApiResponse.ok(reorderService.importExcel(file));
    }

    @GetMapping("/stock-alerts")
    public ApiResponse<FePage<StockAlertDto>> listAlerts(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String alertType) {
        return ApiResponse.ok(reorderService.listAlerts(status, alertType));
    }

    @PostMapping("/stock-alerts/{id}/dismiss")
    public ApiResponse<StockAlertDto> dismiss(@PathVariable String id) {
        return ApiResponse.ok(reorderService.dismiss(id));
    }
}
