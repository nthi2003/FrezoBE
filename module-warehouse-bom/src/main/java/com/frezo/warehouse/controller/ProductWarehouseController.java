package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.warehouse.service.ProductWarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/warehouse/product")
@RequiredArgsConstructor
@Tag(name = "Warehouse — Product context", description = "Tích hợp sản phẩm ↔ kho: biểu đồ tồn, tồn theo kho, quy tắc đặt hàng")
public class ProductWarehouseController {

    private final ProductWarehouseService productWarehouseService;

    @GetMapping("/{productId}/context")
    @CheckPermission(api = "/warehouse/product/{productId}/context", action = "VIEW")
    @Operation(summary = "Ngữ cảnh kho cho sản phẩm", description = "Tồn theo kho, biểu đồ biến động, quy tắc reorder")
    public ApiResponse<?> getContext(
            @PathVariable String productId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) String warehouseId) {
        return ApiResponse.success(productWarehouseService.getContext(productId, days, warehouseId));
    }

    @GetMapping("/{productId}/stock-chart")
    @CheckPermission(api = "/warehouse/product/{productId}/stock-chart", action = "VIEW")
    @Operation(summary = "Biểu đồ tồn kho theo thời gian")
    public ApiResponse<?> getStockChart(
            @PathVariable String productId,
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) String warehouseId) {
        return ApiResponse.success(productWarehouseService.getStockChart(productId, days, warehouseId));
    }

    @GetMapping("/{productId}/stock-levels")
    @CheckPermission(api = "/warehouse/product/{productId}/stock-levels", action = "VIEW")
    @Operation(summary = "Tồn kho theo từng kho")
    public ApiResponse<?> getStockLevels(@PathVariable String productId) {
        return ApiResponse.success(productWarehouseService.getStockLevels(productId));
    }

    @GetMapping("/{productId}/reorder-rules")
    @CheckPermission(api = "/warehouse/product/{productId}/reorder-rules", action = "VIEW")
    @Operation(summary = "Quy tắc đặt hàng của sản phẩm")
    public ApiResponse<?> getReorderRules(@PathVariable String productId) {
        return ApiResponse.success(productWarehouseService.getReorderRules(productId));
    }
}
