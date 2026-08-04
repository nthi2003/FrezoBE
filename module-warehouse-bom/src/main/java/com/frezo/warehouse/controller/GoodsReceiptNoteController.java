package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.warehouse.dto.request.GrnConfirmRequest;
import com.frezo.warehouse.dto.request.GrnCreateRequest;
import com.frezo.warehouse.dto.request.GrnUpdateRequest;
import com.frezo.warehouse.service.DocumentPrintService;
import com.frezo.warehouse.service.GoodsReceiptNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/warehouse/grn")
@RequiredArgsConstructor
@Tag(name = "9. Quản lý nhập kho", description = "API phiếu nhập kho (GRN)")
public class GoodsReceiptNoteController {

    private final GoodsReceiptNoteService grnService;
    private final DocumentPrintService documentPrintService;

    @Operation(summary = "Tạo phiếu nhập kho", description = "Tạo GRN từ PO hoặc nhập tay")
    @PostMapping
    @CheckPermission(api = "/warehouse/grn", action = "CREATE")
    public ApiResponse<?> create(@Valid @RequestBody GrnCreateRequest request) {
        return ApiResponse.success(grnService.create(request));
    }

    @Operation(summary = "Cập nhật phiếu nhập kho", description = "Sửa HĐ NCC, ghi chú — chỉ khi chưa CONFIRMED")
    @PutMapping("/{id}")
    @CheckPermission(api = "/warehouse/grn/{id}", action = "UPDATE")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody GrnUpdateRequest request) {
        return ApiResponse.success(grnService.update(id, request));
    }

    @Operation(summary = "Gửi duyệt phiếu nhập kho", description = "DRAFT → PENDING_APPROVAL")
    @PostMapping("/{id}/submit")
    @CheckPermission(api = "/warehouse/grn/{id}/submit", action = "UPDATE")
    public ApiResponse<?> submit(@PathVariable String id) {
        return ApiResponse.success(grnService.submit(id));
    }

    @Operation(summary = "Duyệt phiếu nhập kho", description = "PENDING_APPROVAL/DRAFT → APPROVED")
    @PostMapping("/{id}/approve")
    @CheckPermission(api = "/warehouse/grn/{id}/approve", action = "UPDATE")
    public ApiResponse<?> approve(@PathVariable String id) {
        return ApiResponse.success(grnService.approve(id));
    }

    @Operation(summary = "Xác nhận nhập kho", description = "APPROVED/DRAFT → CONFIRMED — cập nhật stock")
    @PostMapping("/{id}/confirm")
    @CheckPermission(api = "/warehouse/grn/{id}/confirm", action = "UPDATE")
    public ApiResponse<?> confirm(@PathVariable String id, @RequestBody GrnConfirmRequest request) {
        return ApiResponse.success(grnService.confirm(id, request));
    }

    @Operation(summary = "Huỷ phiếu nhập kho")
    @PostMapping("/{id}/cancel")
    @CheckPermission(api = "/warehouse/grn/{id}/cancel", action = "UPDATE")
    public ApiResponse<?> cancel(@PathVariable String id, @RequestParam(required = false) String reason) {
        grnService.cancel(id, reason);
        return ApiResponse.success("Huỷ phiếu nhập kho thành công");
    }

    @Operation(
            summary = "Biến động giá nhập NCC theo sản phẩm",
            description = "Trả về chuỗi unit_cost theo thời gian từ dòng phiếu nhập kho (bỏ phiếu CANCELLED)")
    @GetMapping("/product/{productId}/price-history")
    @CheckPermission(api = "/warehouse/grn/product/{productId}/price-history", action = "VIEW")
    public ApiResponse<?> getProductPriceHistory(@PathVariable String productId) {
        return ApiResponse.success(grnService.getProductPriceHistory(productId));
    }

    @Operation(summary = "Chi tiết phiếu nhập kho")
    @GetMapping("/{id}")
    @CheckPermission(api = "/warehouse/grn/{id}", action = "VIEW")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(grnService.getById(id));
    }

    @Operation(summary = "Tra cứu theo mã GRN")
    @GetMapping("/code/{grnCode}")
    @CheckPermission(api = "/warehouse/grn/code/{grnCode}", action = "VIEW")
    public ApiResponse<?> getByCode(@PathVariable String grnCode) {
        return ApiResponse.success(grnService.getByCode(grnCode));
    }

    @Operation(summary = "Danh sách phiếu nhập kho")
    @GetMapping
    @CheckPermission(api = "/warehouse/grn", action = "VIEW")
    public ApiResponse<?> filter(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(grnService.filter(status, keyword, page, size));
    }

    @Operation(summary = "Xoá phiếu nhập kho (chỉ DRAFT)")
    @DeleteMapping("/{id}")
    @CheckPermission(api = "/warehouse/grn/{id}", action = "DELETE")
    public ApiResponse<?> delete(@PathVariable String id) {
        grnService.delete(id);
        return ApiResponse.success("Xoá phiếu nhập kho thành công");
    }

    @Operation(summary = "In phiếu nhập kho", description = "Trả về HTML để in/kết xuất PDF")
    @GetMapping(value = "/{id}/print", produces = MediaType.TEXT_HTML_VALUE)
    @CheckPermission(api = "/warehouse/grn/{id}/print", action = "VIEW")
    public String print(@PathVariable String id) {
        return documentPrintService.printGrn(id);
    }
}
