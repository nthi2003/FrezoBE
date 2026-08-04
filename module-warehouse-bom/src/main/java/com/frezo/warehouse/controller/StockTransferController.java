package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.warehouse.dto.request.TransferConfirmRequest;
import com.frezo.warehouse.dto.request.TransferCreateRequest;
import com.frezo.warehouse.service.DocumentPrintService;
import com.frezo.warehouse.service.StockTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/warehouse/transfer")
@RequiredArgsConstructor
@Tag(name = "33. Chuyển kho", description = "API chuyển kho nội bộ")
public class StockTransferController {

    private final StockTransferService transferService;
    private final DocumentPrintService documentPrintService;

    @Operation(summary = "Tạo phiếu chuyển kho")
    @PostMapping
    @CheckPermission(api = "/warehouse/transfer", action = "CREATE")
    public ApiResponse<?> create(@Valid @RequestBody TransferCreateRequest request) {
        return ApiResponse.success(transferService.create(request));
    }

    @Operation(summary = "Xác nhận chuyển kho", description = "Trừ kho nguồn, cộng kho đích, ghi stock_ledger")
    @PostMapping("/{id}/confirm")
    @CheckPermission(api = "/warehouse/transfer/{id}/confirm", action = "UPDATE")
    public ApiResponse<?> confirm(@PathVariable String id, @RequestBody TransferConfirmRequest request) {
        return ApiResponse.success(transferService.confirm(id, request));
    }

    @Operation(summary = "Huỷ phiếu chuyển kho")
    @PostMapping("/{id}/cancel")
    @CheckPermission(api = "/warehouse/transfer/{id}/cancel", action = "UPDATE")
    public ApiResponse<?> cancel(@PathVariable String id, @RequestParam(required = false) String reason) {
        transferService.cancel(id, reason);
        return ApiResponse.success("Huỷ phiếu chuyển kho thành công");
    }

    @Operation(summary = "Chi tiết phiếu chuyển kho")
    @GetMapping("/{id}")
    @CheckPermission(api = "/warehouse/transfer/{id}", action = "VIEW")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(transferService.getById(id));
    }

    @Operation(summary = "Tra cứu theo mã chuyển kho")
    @GetMapping("/code/{transferCode}")
    @CheckPermission(api = "/warehouse/transfer/code/{transferCode}", action = "VIEW")
    public ApiResponse<?> getByCode(@PathVariable String transferCode) {
        return ApiResponse.success(transferService.getByCode(transferCode));
    }

    @Operation(summary = "Danh sách phiếu chuyển kho")
    @GetMapping
    @CheckPermission(api = "/warehouse/transfer", action = "VIEW")
    public ApiResponse<?> filter(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(transferService.filter(status, keyword, page, size));
    }

    @Operation(summary = "Xoá phiếu chuyển kho (chỉ DRAFT)")
    @DeleteMapping("/{id}")
    @CheckPermission(api = "/warehouse/transfer/{id}", action = "DELETE")
    public ApiResponse<?> delete(@PathVariable String id) {
        transferService.delete(id);
        return ApiResponse.success("Xoá phiếu chuyển kho thành công");
    }

    @Operation(summary = "In phiếu chuyển kho", description = "Trả về HTML để in/kết xuất PDF")
    @GetMapping(value = "/{id}/print", produces = MediaType.TEXT_HTML_VALUE)
    @CheckPermission(api = "/warehouse/transfer/{id}/print", action = "VIEW")
    public String print(@PathVariable String id) {
        return documentPrintService.printTransfer(id);
    }
}
