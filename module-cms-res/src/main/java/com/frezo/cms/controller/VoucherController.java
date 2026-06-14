package com.frezo.cms.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.cms.entity.Vouchers;
import com.frezo.cms.service.VoucherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/cms/voucher")
@RequiredArgsConstructor
@Tag(name = "Voucher API", description = "Voucher management in CMS")
public class VoucherController {

    private final VoucherService voucherService;

    @PostMapping
    @Operation(summary = "Create a new voucher")
    public ApiResponse<Vouchers> create(@RequestBody Vouchers voucher) {
        return ApiResponse.success(voucherService.create(voucher));
    }

    @GetMapping
    @Operation(summary = "Get all vouchers with pagination")
    public ApiResponse<Map<String, Object>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.success(voucherService.getAll(page, size));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update voucher status (active/inactive)")
    public ApiResponse<Vouchers> updateStatus(@PathVariable String id, @RequestParam boolean isActive) {
        return ApiResponse.success(voucherService.updateStatus(id, isActive));
    }
}
