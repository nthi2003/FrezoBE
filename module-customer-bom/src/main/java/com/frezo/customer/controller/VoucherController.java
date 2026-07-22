package com.frezo.customer.controller;

import com.frezo.customer.entity.Voucher;
import com.frezo.customer.service.impl.VoucherServiceImpl;
import com.frezo.util.web.Response;
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
    public Response<?> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String discountType,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        return Response.ok(voucherService.getAll(keyword, status, discountType, pageNumber, pageSize));
    }

    @Operation(summary = "Tạo voucher mới")
    @PostMapping
    public Response<?> create(@RequestBody Voucher request) {
        return Response.ok(voucherService.create(request));
    }

    @Operation(summary = "Cập nhật voucher")
    @PutMapping("/{id}")
    public Response<?> update(@PathVariable String id, @RequestBody Voucher request) {
        return Response.ok(voucherService.update(id, request));
    }

    @Operation(summary = "Xóa voucher")
    @DeleteMapping("/{id}")
    public Response<?> delete(@PathVariable String id) {
        voucherService.delete(id);
        return Response.ok();
    }

    @Operation(summary = "Kiểm tra mã voucher hợp lệ")
    @GetMapping("/validate")
    public Response<?> validate(
            @RequestParam String code,
            @RequestParam BigDecimal orderValue) {
        return Response.ok(voucherService.validate(code, orderValue));
    }
}
