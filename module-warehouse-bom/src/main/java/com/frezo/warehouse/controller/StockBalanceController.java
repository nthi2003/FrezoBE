package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.warehouse.service.StockBalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/warehouse/stock")
@RequiredArgsConstructor
@Tag(name = "32. Tồn kho", description = "API xem tồn kho, cảnh báo tồn thấp, xuất Excel")
public class StockBalanceController {

    private final StockBalanceService stockBalanceService;

    @Operation(summary = "Xem tồn kho", description = "Lọc theo kho, sản phẩm")
    @GetMapping
    public ApiResponse<?> filter(
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(stockBalanceService.filter(warehouseId, productId, keyword, page, size));
    }

    @Operation(summary = "Chi tiết tồn kho")
    @GetMapping("/{id}")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(stockBalanceService.getById(id));
    }

    @Operation(summary = "Cảnh báo tồn kho thấp", description = "Sản phẩm có quantityAvailable <= warningThreshold")
    @GetMapping("/alerts")
    public ApiResponse<?> getLowStockAlerts() {
        return ApiResponse.success(stockBalanceService.getLowStockAlerts());
    }

    @Operation(summary = "Thống kê kho nhanh", description = "Tổng số kho, sản phẩm, GRN/GIN hôm nay")
    @GetMapping("/stats")
    public ApiResponse<?> getStats() {
        return ApiResponse.success(stockBalanceService.getStats());
    }

    @Operation(summary = "Xuất Excel tồn kho")
    @GetMapping("/export")
    public void exportStock(
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String productId,
            HttpServletResponse response) throws IOException {
        byte[] data = stockBalanceService.exportStock(warehouseId, productId);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=ton_kho.xlsx");
        response.getOutputStream().write(data);
    }
}
