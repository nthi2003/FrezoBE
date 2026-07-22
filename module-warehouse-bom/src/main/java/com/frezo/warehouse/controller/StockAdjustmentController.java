package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.warehouse.dto.request.StockAdjustmentCreateRequest;
import com.frezo.warehouse.service.StockAdjustmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouse/adjustment")
@RequiredArgsConstructor
@Tag(name = "34. Kiểm kê & Điều chỉnh tồn", description = "API kiểm kê kho, điều chỉnh chênh lệch tồn")
public class StockAdjustmentController {

    private final StockAdjustmentService adjustmentService;

    @Operation(summary = "Tạo phiếu kiểm kê / điều chỉnh tồn")
    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody StockAdjustmentCreateRequest request) {
        return ApiResponse.success(adjustmentService.create(request));
    }

    @Operation(summary = "Xác nhận điều chỉnh", description = "CONFIRM → cập nhật stock_balance + stock_ledger")
    @PostMapping("/{id}/confirm")
    public ApiResponse<?> confirm(@PathVariable String id) {
        return ApiResponse.success(adjustmentService.confirm(id));
    }

    @Operation(summary = "Huỷ phiếu điều chỉnh")
    @PostMapping("/{id}/cancel")
    public ApiResponse<?> cancel(@PathVariable String id, @RequestParam(required = false) String reason) {
        adjustmentService.cancel(id, reason);
        return ApiResponse.success("Huỷ phiếu điều chỉnh thành công");
    }

    @Operation(summary = "Chi tiết phiếu điều chỉnh")
    @GetMapping("/{id}")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(adjustmentService.getById(id));
    }

    @Operation(summary = "Tra cứu theo mã")
    @GetMapping("/code/{code}")
    public ApiResponse<?> getByCode(@PathVariable String code) {
        return ApiResponse.success(adjustmentService.getByCode(code));
    }

    @Operation(summary = "Danh sách phiếu điều chỉnh")
    @GetMapping
    public ApiResponse<?> filter(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String warehouseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(adjustmentService.filter(status, warehouseId, page, size));
    }

    @Operation(summary = "Xoá phiếu điều chỉnh (chỉ DRAFT)")
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        adjustmentService.delete(id);
        return ApiResponse.success("Xoá phiếu điều chỉnh thành công");
    }
}
