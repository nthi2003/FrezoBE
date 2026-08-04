package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
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
    @CheckPermission(api = "/warehouse/shrinkage", action = "CREATE")
    public ApiResponse<?> create(@Valid @RequestBody ShrinkageCreateRequest request) {
        return ApiResponse.success(shrinkageService.create(request));
    }

    @Operation(summary = "Xác nhận hao hụt — trừ tồn lô")
    @PostMapping("/{id}/confirm")
    @CheckPermission(api = "/warehouse/shrinkage/{id}/confirm", action = "UPDATE")
    public ApiResponse<?> confirm(@PathVariable String id) {
        return ApiResponse.success(shrinkageService.confirm(id));
    }

    @Operation(summary = "Huỷ phiếu hao hụt")
    @PostMapping("/{id}/cancel")
    @CheckPermission(api = "/warehouse/shrinkage/{id}/cancel", action = "UPDATE")
    public ApiResponse<?> cancel(@PathVariable String id, @RequestParam(required = false) String reason) {
        shrinkageService.cancel(id, reason);
        return ApiResponse.success("Huỷ phiếu hao hụt thành công");
    }

    @Operation(summary = "Chi tiết phiếu hao hụt")
    @GetMapping("/{id}")
    @CheckPermission(api = "/warehouse/shrinkage/{id}", action = "VIEW")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(shrinkageService.getById(id));
    }

    @Operation(summary = "Danh sách phiếu hao hụt")
    @GetMapping
    @CheckPermission(api = "/warehouse/shrinkage", action = "VIEW")
    public ApiResponse<?> list(
            @RequestParam(required = false) String warehouseId,
            @RequestParam(required = false) String status) {
        return ApiResponse.success(shrinkageService.list(warehouseId, status));
    }
}
