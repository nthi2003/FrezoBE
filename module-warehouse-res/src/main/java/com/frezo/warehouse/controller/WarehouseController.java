package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.warehouse.dto.request.WarehouseCreateRequest;
import com.frezo.warehouse.dto.request.WarehouseUpdateRequest;
import com.frezo.warehouse.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/warehouse")
@RequiredArgsConstructor
@Tag(name = "30. Nhập kho", description = "API quản lý kho, zone, location")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @Operation(summary = "Danh sách kho")
    @GetMapping
    public ApiResponse<?> getAll() {
        return ApiResponse.success(warehouseService.getAll());
    }

    @Operation(summary = "Tìm kiếm kho")
    @GetMapping("/search")
    public ApiResponse<?> filter(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(warehouseService.filter(keyword, status, page, size));
    }

    @Operation(summary = "Chi tiết kho")
    @GetMapping("/{id}")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(warehouseService.getById(id));
    }

    @Operation(summary = "Tra cứu theo mã kho")
    @GetMapping("/code/{code}")
    public ApiResponse<?> getByCode(@PathVariable String code) {
        return ApiResponse.success(warehouseService.getByCode(code));
    }

    @Operation(summary = "Thêm kho mới")
    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody WarehouseCreateRequest request) {
        return ApiResponse.success(warehouseService.create(request));
    }

    @Operation(summary = "Cập nhật kho")
    @PutMapping("/{id}")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody WarehouseUpdateRequest request) {
        return ApiResponse.success(warehouseService.update(id, request));
    }

    @Operation(summary = "Xoá kho")
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        warehouseService.delete(id);
        return ApiResponse.success("Xoá kho thành công");
    }
}
