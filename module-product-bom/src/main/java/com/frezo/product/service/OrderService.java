package com.frezo.product.service;

import com.frezo.product.entity.CartItem;
import com.frezo.product.entity.SaleOrder;
import com.frezo.product.repository.SaleOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final SaleOrderRepository saleOrderRepository;
    private final CartService cartService;

    @Transactional
    public SaleOrder createOrderFromCart(String customerId, String staffId, String paymentMethod) {
        List<CartItem> items = cartService.getCartItems(customerId);
        if (items.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        Double totalAmount = items.stream()
                .mapToDouble(item -> item.getUnitPrice() * item.getQuantity())
                .sum();

        SaleOrder order = SaleOrder.builder()
                .customerId(customerId)
                .staffId(staffId)
                .orderCode("ORD-" + System.currentTimeMillis())
                .totalAmount(totalAmount)
                .paymentMethod(paymentMethod)
                .paymentStatus("PENDING")
                .build();

        SaleOrder savedOrder = saleOrderRepository.save(order);
        
        // Clear cart after order creation
        cartService.clearCart(customerId);
        
        return savedOrder;
    }

    public List<SaleOrder> getAllOrders() {
        return saleOrderRepository.findAll();
    }
}
