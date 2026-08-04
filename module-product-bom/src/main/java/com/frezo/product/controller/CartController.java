package com.frezo.product.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.product.entity.CartItem;
import com.frezo.product.entity.Carts;
import com.frezo.product.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/product/cart")
@RequiredArgsConstructor
@Tag(name = "Cart API", description = "Shopping cart management")
public class CartController {

    private final CartService cartService;

    @GetMapping("/{customerId}")
    @CheckPermission(api = "/product/cart/{customerId}", action = "VIEW")
    @Operation(summary = "Get current cart for customer")
    public ApiResponse<List<CartItem>> getCart(@PathVariable String customerId) {
        return ApiResponse.success(cartService.getCartItems(customerId));
    }

    @PostMapping("/{customerId}/add")
    @CheckPermission(api = "/product/cart/{customerId}/add", action = "CREATE")
    @Operation(summary = "Add product to cart")
    public ApiResponse<CartItem> addToCart(
            @PathVariable String customerId,
            @RequestParam String productId,
            @RequestParam Integer quantity,
            @RequestParam Double price) {
        return ApiResponse.success(cartService.addToCart(customerId, productId, quantity, price));
    }

    @DeleteMapping("/{customerId}/clear")
    @CheckPermission(api = "/product/cart/{customerId}/clear", action = "DELETE")
    @Operation(summary = "Clear customer cart")
    public ApiResponse<String> clearCart(@PathVariable String customerId) {
        cartService.clearCart(customerId);
        return ApiResponse.success("Cart cleared");
    }
}
