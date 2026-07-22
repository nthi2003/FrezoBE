package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.warehouse.dto.request.WarehouseLocationRequest;
import com.frezo.warehouse.service.WarehouseLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/warehouse/location")
@RequiredArgsConstructor
@Tag(name = "30. Nhập kho", description = "API quản lý kho, zone, location")
public class WarehouseLocationController {

    private final WarehouseLocationService locationService;

    @Operation(summary = "Location theo zone")
    @GetMapping("/by-zone/{zoneId}")
    public ApiResponse<?> getByZone(@PathVariable String zoneId) {
        return ApiResponse.success(locationService.getByZoneId(zoneId));
    }

    @Operation(summary = "Chi tiết location")
    @GetMapping("/{id}")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(locationService.getById(id));
    }

    @Operation(summary = "Tra cứu theo barcode")
    @GetMapping("/barcode/{barcode}")
    public ApiResponse<?> getByBarcode(@PathVariable String barcode) {
        return ApiResponse.success(locationService.getByBarcode(barcode));
    }

    @Operation(summary = "Thêm location")
    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody WarehouseLocationRequest request) {
        return ApiResponse.success(locationService.create(request));
    }

    @Operation(summary = "Cập nhật location")
    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody WarehouseLocationRequest request) {
        return ApiResponse.success(locationService.update(id, request));
    }

    @Operation(summary = "Xoá location")
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        locationService.delete(id);
        return ApiResponse.success("Xoá location thành công");
    }
}
