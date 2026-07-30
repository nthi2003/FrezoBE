package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.warehouse.dto.request.ShrinkageCreateRequest;
import com.frezo.warehouse.service.StockShrinkageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/warehouse/shrinkage")
@RequiredArgsConstructor
@Tag(name = "Warehouse — Shrinkage", description = "Ghi nhận hao hụt SHRINK/DAMAGE/EXPIRED")
public class StockShrinkageController {

    private final StockShrinkageService shrinkageService;

    @Operation(summary = "Tạo phiếu hao hụt")
    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody ShrinkageCreateRequest request) {
        return ApiResponse.success(shrinkageService.create(request));
    }

    @Operation(summary = "Xác nhận hao hụt — trừ tồn lô")
    @PostMapping("/{id}/confirm")
    public ApiResponse<?> confirm(@PathVariable String id) {
        return ApiResponse.success(shrinkageService.confirm(id));
    }

    @Operation(summary = "Huỷ phiếu hao hụt")
    @PostMapping("/{id}/cancel")
    public ApiResponse<?> cancel(@PathVariable String id, @RequestParam(required = false) String reason) {
        shrinkageService.cancel(id, reason);
        return ApiResponse.success("Huỷ phiếu hao hụt thành công");
    }

    @Operation(summary = "Chi tiết phiếu hao hụt")
    @GetMapping("/{id}")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(shrinkageService.getById(id));
    }

    @Operation(summary = "Danh sách phiếu hao hụt")
    @GetMapping
    public ApiResponse<?> list(
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(shrinkageService.list(warehouseId, status));
    }
}
