package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.FePage;
import com.frezo.warehouse.dto.request.PurchaseOrderSaveRequest;
import com.frezo.warehouse.dto.response.PurchaseOrderDto;
import com.frezo.warehouse.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/warehouse/purchase-orders")
@RequiredArgsConstructor
@Tag(name = "Warehouse — Purchase Order", description = "PO từ PR APPROVED")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    public ApiResponse<FePage<PurchaseOrderDto>> list() {
        return ApiResponse.ok(purchaseOrderService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<PurchaseOrderDto> get(@PathVariable String id) {
        return ApiResponse.ok(purchaseOrderService.get(id));
    }

    @PostMapping
    public ApiResponse<PurchaseOrderDto> create(@RequestBody PurchaseOrderSaveRequest req) {
        return ApiResponse.ok(purchaseOrderService.create(req));
    }

    @PutMapping("/{id}")
    public ApiResponse<PurchaseOrderDto> update(@PathVariable String id,
                                                @RequestBody PurchaseOrderSaveRequest req) {
        return ApiResponse.ok(purchaseOrderService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        purchaseOrderService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "Tạo PO từ PR APPROVED (idempotent theo prId)")
    @PostMapping("/from-pr/{prId}")
    public ApiResponse<PurchaseOrderDto> fromPr(@PathVariable String prId) {
        return ApiResponse.ok(purchaseOrderService.createFromPr(prId));
    }

    @Operation(summary = "Xác nhận nhận hàng + stub tăng StockBalance")
    @PostMapping("/{id}/confirm-receive")
    public ApiResponse<PurchaseOrderDto> confirmReceive(@PathVariable String id) {
        return ApiResponse.ok(purchaseOrderService.confirmReceive(id));
    }
}
