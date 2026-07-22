package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.warehouse.dto.request.GinConfirmRequest;
import com.frezo.warehouse.dto.request.GinCreateRequest;
import com.frezo.warehouse.service.DocumentPrintService;
import com.frezo.warehouse.service.GoodsIssueNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/warehouse/gin")
@RequiredArgsConstructor
@Tag(name = "31. Xuất kho", description = "API phiếu xuất kho (GIN)")
public class GoodsIssueNoteController {

    private final GoodsIssueNoteService ginService;
    private final DocumentPrintService documentPrintService;

    @Operation(summary = "Tạo phiếu xuất kho")
    @PostMapping
    public ApiResponse<?> create(@Valid @RequestBody GinCreateRequest request) {
        return ApiResponse.success(ginService.create(request));
    }

    @Operation(summary = "Xác nhận xuất kho", description = "CONFIRM → cập nhật stock_ledger, stock_transactions, stock_balances")
    @PostMapping("/{id}/confirm")
    public ApiResponse<?> confirm(@PathVariable String id, @RequestBody GinConfirmRequest request) {
        return ApiResponse.success(ginService.confirm(id, request));
    }

    @Operation(summary = "Xác nhận hàng loạt")
    @PostMapping("/batch-confirm")
    public ApiResponse<?> batchConfirm(@RequestBody List<String> ids) {
        ginService.batchConfirm(ids);
        return ApiResponse.success("Xác nhận " + ids.size() + " phiếu xuất kho thành công");
    }

    @Operation(summary = "Huỷ phiếu xuất kho")
    @PostMapping("/{id}/cancel")
    public ApiResponse<?> cancel(@PathVariable String id, @RequestParam(required = false) String reason) {
        ginService.cancel(id, reason);
        return ApiResponse.success("Huỷ phiếu xuất kho thành công");
    }

    @Operation(summary = "Huỷ hàng loạt")
    @PostMapping("/batch-cancel")
    public ApiResponse<?> batchCancel(@RequestBody List<String> ids, @RequestParam(required = false) String reason) {
        ginService.batchCancel(ids, reason);
        return ApiResponse.success("Huỷ " + ids.size() + " phiếu xuất kho thành công");
    }

    @Operation(summary = "Chi tiết phiếu xuất kho")
    @GetMapping("/{id}")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(ginService.getById(id));
    }

    @Operation(summary = "Tra cứu theo mã GIN")
    @GetMapping("/code/{ginCode}")
    public ApiResponse<?> getByCode(@PathVariable String ginCode) {
        return ApiResponse.success(ginService.getByCode(ginCode));
    }

    @Operation(summary = "Danh sách phiếu xuất kho")
    @GetMapping
    public ApiResponse<?> filter(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(ginService.filter(status, keyword, page, size));
    }

    @Operation(summary = "Xoá phiếu xuất kho (chỉ DRAFT)")
    @DeleteMapping("/{id}")
    public ApiResponse<?> delete(@PathVariable String id) {
        ginService.delete(id);
        return ApiResponse.success("Xoá phiếu xuất kho thành công");
    }

    @Operation(summary = "In phiếu xuất kho", description = "Trả về HTML để in/kết xuất PDF")
    @GetMapping(value = "/{id}/print", produces = MediaType.TEXT_HTML_VALUE)
    public String print(@PathVariable String id) {
        return documentPrintService.printGin(id);
    }

    @Operation(summary = "Xuất Excel phiếu xuất kho")
    @GetMapping("/{id}/export")
    public void exportExcel(@PathVariable String id, HttpServletResponse response) throws IOException {
        byte[] data = documentPrintService.exportGinExcel(id);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=phieu_xuat_kho.xlsx");
        response.getOutputStream().write(data);
    }
}
