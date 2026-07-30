package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.warehouse.service.StockBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/warehouse/batches")
@RequiredArgsConstructor
@Tag(name = "Warehouse — Batch/Lot", description = "Quản lý lô hàng SME rau củ")
public class StockBatchController {

    private final StockBatchService batchService;

    @Operation(summary = "Danh sách lô theo kho/SP")
    @GetMapping
    public ApiResponse<?> list(
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String productId) {
        return ApiResponse.success(batchService.list(warehouseId, productId));
    }

    @Operation(summary = "Chi tiết lô")
    @GetMapping("/{id}")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(batchService.getById(id));
    }
}
