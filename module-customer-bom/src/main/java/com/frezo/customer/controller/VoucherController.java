package com.frezo.customer.controller;

import com.frezo.customer.entity.Voucher;
import com.frezo.customer.service.impl.VoucherServiceImpl;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/voucher")
@RequiredArgsConstructor
@Tag(name = "Quản lý Voucher & Khuyến mãi", description = "CRUD voucher – user tự cấu hình")
public class VoucherController {

    private final VoucherServiceImpl voucherService;

    @Operation(summary = "Danh sách voucher")
    @GetMapping
    @CheckPermission(api = "/voucher", action = "VIEW")
    public ApiResponse<?> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String discountType,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return ApiResponse.ok(voucherService.getAll(keyword, status, discountType, pageNumber, pageSize));
    }

    @Operation(summary = "Tạo voucher mới")
    @PostMapping
    @CheckPermission(api = "/voucher", action = "CREATE")
    public ApiResponse<?> create(@RequestBody Voucher request) {
        return ApiResponse.ok(voucherService.create(request));
    }

    @Operation(summary = "Cập nhật voucher")
    @PutMapping("/{id}")
    @CheckPermission(api = "/voucher/{id}", action = "UPDATE")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody Voucher request) {
        return ApiResponse.ok(voucherService.update(id, request));
    }

    @Operation(summary = "Xóa voucher")
    @DeleteMapping("/{id}")
    @CheckPermission(api = "/voucher/{id}", action = "DELETE")
    public ApiResponse<?> delete(@PathVariable String id) {
        voucherService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "Kiểm tra mã voucher hợp lệ")
    @GetMapping("/validate")
    @CheckPermission(api = "/voucher/validate", action = "VIEW")
    public ApiResponse<?> validate(
            @RequestParam String code,
            @RequestParam BigDecimal orderValue) {
        return ApiResponse.ok(voucherService.validate(code, orderValue));
    }
}
