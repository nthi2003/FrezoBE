package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
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
    @CheckPermission(api = "/warehouse/location/by-zone/{zoneId}", action = "VIEW")
    public ApiResponse<?> getByZone(@PathVariable String zoneId) {
        return ApiResponse.success(locationService.getByZoneId(zoneId));
    }

    @Operation(summary = "Location theo kho (tất cả zone)")
    @GetMapping("/by-warehouse/{warehouseId}")
    @CheckPermission(api = "/warehouse/location/by-warehouse/{warehouseId}", action = "VIEW")
    public ApiResponse<?> getByWarehouse(@PathVariable String warehouseId) {
        return ApiResponse.success(locationService.getByWarehouseId(warehouseId));
    }

    @Operation(summary = "Chi tiết location")
    @GetMapping("/{id}")
    @CheckPermission(api = "/warehouse/location/{id}", action = "VIEW")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(locationService.getById(id));
    }

    @Operation(summary = "Tra cứu theo barcode")
    @GetMapping("/barcode/{barcode}")
    @CheckPermission(api = "/warehouse/location/barcode/{barcode}", action = "VIEW")
    public ApiResponse<?> getByBarcode(@PathVariable String barcode) {
        return ApiResponse.success(locationService.getByBarcode(barcode));
    }

    @Operation(summary = "Thêm location")
    @PostMapping
    @CheckPermission(api = "/warehouse/location", action = "CREATE")
    public ApiResponse<?> create(@Valid @RequestBody WarehouseLocationRequest request) {
        return ApiResponse.success(locationService.create(request));
    }

    @Operation(summary = "Cập nhật location")
    @PutMapping("/{id}")
    @CheckPermission(api = "/warehouse/location/{id}", action = "UPDATE")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody WarehouseLocationRequest request) {
        return ApiResponse.success(locationService.update(id, request));
    }

    @Operation(summary = "Xoá location")
    @DeleteMapping("/{id}")
    @CheckPermission(api = "/warehouse/location/{id}", action = "DELETE")
    public ApiResponse<?> delete(@PathVariable String id) {
        locationService.delete(id);
        return ApiResponse.success("Xoá location thành công");
    }
}
