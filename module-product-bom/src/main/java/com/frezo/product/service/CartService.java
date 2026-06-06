package com.frezo.product.service;

import com.frezo.product.entity.CartItem;
import com.frezo.product.entity.Carts;
import com.frezo.product.repository.CartItemRepository;
import com.frezo.product.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public Carts getOrCreateCart(String customerId) {
        return cartRepository.findByCustomerIdAndStatus(customerId, "ACTIVE")
                .orElseGet(() -> {
                    Carts newCart = Carts.builder()
                            .customerId(customerId)
                            .status("ACTIVE")
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    @Transactional
    public CartItem addToCart(String customerId, String productId, Integer quantity, Double price) {
        Carts cart = getOrCreateCart(customerId);
        
        CartItem item = CartItem.builder()
                .cartId(cart.getId())
                .productId(productId)
                .unitPrice(price)
                .totalPrice(price * quantity)
                .build();
        
        return cartItemRepository.save(item);
    }

    public List<CartItem> getCartItems(String customerId) {
        Carts cart = getOrCreateCart(customerId);
        return cartItemRepository.findByCartId(cart.getId());
    }

    @Transactional
    public void clearCart(String customerId) {
        Carts cart = getOrCreateCart(customerId);
        cartItemRepository.deleteByCartId(cart.getId());
        cart.setStatus("COMPLETED");
        cartRepository.save(cart);
    }
}
