package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.warehouse.dto.request.GrnConfirmRequest;
import com.frezo.warehouse.dto.request.GrnCreateRequest;
import com.frezo.warehouse.service.GoodsReceiptNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/warehouse/grn")
@RequiredArgsConstructor
@Tag(name = "9. Quản lý nhập kho", description = "API phiếu nhập kho (GRN)")
public class GoodsReceiptNoteController {

    private final GoodsReceiptNoteService grnService;

    @Operation(summary = "Tạo phiếu nhập kho", description = "Tạo GRN từ PO hoặc nhập tay")
    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody GrnCreateRequest request) {
        return ApiResponse.success(grnService.create(request));
    }

    @Operation(summary = "Xác nhận nhập kho", description = "CONFIRM → cập nhật stock_ledger, stock_transactions, stock_balances")
    @PostMapping("/{id}/confirm")
    public ApiResponse<?> confirm(@PathVariable String id, @RequestBody GrnConfirmRequest request) {
        return ApiResponse.success(grnService.confirm(id, request));
    }

    @Operation(summary = "Huỷ phiếu nhập kho")
    @PostMapping("/{id}/cancel")
    public ApiResponse<?> cancel(@PathVariable String id, @RequestParam(required = false) String reason) {
        grnService.cancel(id, reason);
        return ApiResponse.success("Huỷ phiếu nhập kho thành công");
    }

    @Operation(summary = "Chi tiết phiếu nhập kho")
    @GetMapping("/{id}")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(grnService.getById(id));
    }

    @Operation(summary = "Tra cứu theo mã GRN")
    @GetMapping("/code/{grnCode}")
    public ApiResponse<?> getByCode(@PathVariable String grnCode) {
        return ApiResponse.success(grnService.getByCode(grnCode));
    }

    @Operation(summary = "Danh sách phiếu nhập kho")
    @GetMapping
    public ApiResponse<?> filter(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(grnService.filter(status, keyword, page, size));
    }

    @Operation(summary = "Xoá phiếu nhập kho (chỉ DRAFT)")
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        grnService.delete(id);
        return ApiResponse.success("Xoá phiếu nhập kho thành công");
    }
}
