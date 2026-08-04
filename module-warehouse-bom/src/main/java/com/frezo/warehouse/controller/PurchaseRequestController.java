package com.frezo.warehouse.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.common.response.FePage;
import com.frezo.warehouse.dto.request.FromAlertsRequest;
import com.frezo.warehouse.dto.request.PurchaseRequestSaveRequest;
import com.frezo.warehouse.dto.response.PurchaseRequestDto;
import com.frezo.warehouse.service.PurchaseRequestService;
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

import java.util.List;

@RestController
@RequestMapping("/warehouse/purchase-requests")
@RequiredArgsConstructor
@Tag(name = "Warehouse — Purchase Request", description = "PR từ StockAlert + Approval")
public class PurchaseRequestController {

    private final PurchaseRequestService purchaseRequestService;

    @Operation(summary = "Sinh PR từ StockAlert (group theo supplier)")
    @PostMapping("/from-alerts")
    @CheckPermission(api = "/warehouse/purchase-requests/from-alerts", action = "CREATE")
    public ApiResponse<List<PurchaseRequestDto>> fromAlerts(@RequestBody FromAlertsRequest req) {
        return ApiResponse.ok(purchaseRequestService.createFromAlerts(req));
    }

    @GetMapping
    @CheckPermission(api = "/warehouse/purchase-requests", action = "VIEW")
    public ApiResponse<FePage<PurchaseRequestDto>> list() {
        return ApiResponse.ok(purchaseRequestService.list());
    }

    @GetMapping("/{id}")
    @CheckPermission(api = "/warehouse/purchase-requests/{id}", action = "VIEW")
    public ApiResponse<PurchaseRequestDto> get(@PathVariable String id) {
        return ApiResponse.ok(purchaseRequestService.get(id));
    }

    @PostMapping
    @CheckPermission(api = "/warehouse/purchase-requests", action = "CREATE")
    public ApiResponse<PurchaseRequestDto> create(@RequestBody PurchaseRequestSaveRequest req) {
        return ApiResponse.ok(purchaseRequestService.create(req));
    }

    @PutMapping("/{id}")
    @CheckPermission(api = "/warehouse/purchase-requests/{id}", action = "UPDATE")
    public ApiResponse<PurchaseRequestDto> update(@PathVariable String id,
                                                  @RequestBody PurchaseRequestSaveRequest req) {
        return ApiResponse.ok(purchaseRequestService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/warehouse/purchase-requests/{id}", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable String id) {
        purchaseRequestService.delete(id);
        return ApiResponse.ok();
    }

    @Operation(summary = "Submit → Approval PURCHASE_REQUEST")
    @PostMapping("/{id}/submit")
    @CheckPermission(api = "/warehouse/purchase-requests/{id}/submit", action = "UPDATE")
    public ApiResponse<PurchaseRequestDto> submit(@PathVariable String id) {
        return ApiResponse.ok(purchaseRequestService.submit(id));
    }
}
