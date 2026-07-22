package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.warehouse.dto.request.WarehouseZoneRequest;
import com.frezo.warehouse.service.WarehouseZoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/warehouse/zone")
@RequiredArgsConstructor
@Tag(name = "30. Nhập kho", description = "API quản lý kho, zone, location")
public class WarehouseZoneController {

    private final WarehouseZoneService zoneService;

    @Operation(summary = "Zone theo kho")
    @GetMapping("/by-warehouse/{warehouseId}")
    public ApiResponse<?> getByWarehouse(@PathVariable String warehouseId) {
        return ApiResponse.success(zoneService.getByWarehouseId(warehouseId));
    }

    @Operation(summary = "Chi tiết zone")
    @GetMapping("/{id}")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(zoneService.getById(id));
    }

    @Operation(summary = "Thêm zone")
    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody WarehouseZoneRequest request) {
        return ApiResponse.success(zoneService.create(request));
    }

    @Operation(summary = "Cập nhật zone")
    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody WarehouseZoneRequest request) {
        return ApiResponse.success(zoneService.update(id, request));
    }

    @Operation(summary = "Xoá zone")
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        zoneService.delete(id);
        return ApiResponse.success("Xoá zone thành công");
    }
}
