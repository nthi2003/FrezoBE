package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.warehouse.service.StockBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/warehouse/batches")
@RequiredArgsConstructor
@Tag(name = "Warehouse — Batch/Lot", description = "Quản lý lô hàng SME rau củ")
public class StockBatchController {

    private final StockBatchService batchService;

    @Operation(summary = "Danh sách lô theo kho/SP + status/keyword/HSD")
    @GetMapping
    @CheckPermission(api = "/warehouse/batches", action = "VIEW")
    public ApiResponse<?> list(
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiryTo) {
        return ApiResponse.success(batchService.list(
                warehouseId, productId, status, keyword, expiryFrom, expiryTo));
    }

    @Operation(summary = "Chi tiết lô")
    @GetMapping("/{id}")
    @CheckPermission(api = "/warehouse/batches/{id}", action = "VIEW")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(batchService.getById(id));
    }
}
