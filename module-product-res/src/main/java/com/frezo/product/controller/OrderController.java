package com.frezo.product.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.product.entity.SaleOrder;
import com.frezo.product.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order API", description = "Order management")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout/{customerId}")
    @Operation(summary = "Create order from current cart")
    public ApiResponse<SaleOrder> checkout(
            @PathVariable String customerId,
            @RequestParam String staffId,
            @RequestParam(defaultValue = "CASH") String paymentMethod) {
        return ApiResponse.success(orderService.createOrderFromCart(customerId, staffId, paymentMethod));
    }

    @GetMapping
    @Operation(summary = "Get all sales orders")
    public ApiResponse<java.util.List<SaleOrder>> getAll() {
        return ApiResponse.success(orderService.getAllOrders());
    }
}
