package com.frezo.cms.controller;

import com.frezo.cms.entity.SupplyOrder;
import com.frezo.common.response.ApiResponse;
import com.frezo.cms.service.OrderPaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cms/orders")
@RequiredArgsConstructor
@Tag(name = "CMS Order API", description = "Order management for CMS")
public class CmsOrderController {

    private final OrderPaymentService orderPaymentService;

    @PostMapping("/payment-status")
    @Operation(summary = "Update payment status for a supply order")
    public ApiResponse<SupplyOrder> updatePaymentStatus(
            @RequestParam String orderId,
            @RequestParam Integer paymentStatus,
            HttpServletRequest request) {
        return ApiResponse.success(orderPaymentService.updatePaymentStatus(orderId, paymentStatus, request));
    }
}
