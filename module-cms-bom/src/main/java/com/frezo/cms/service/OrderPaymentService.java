package com.frezo.cms.service;

import com.frezo.cms.entity.SupplyOrder;

import com.frezo.common.audit.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderPaymentService {

    private final SupplyOrderRepository supplyOrderRepository;
    private final AuditLogService auditLogService;

    public SupplyOrder updatePaymentStatus(String orderId, Integer paymentStatus, HttpServletRequest request) {
        SupplyOrder order = supplyOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Integer oldStatus = order.getPaymentStatus();
        order.setPaymentStatus(paymentStatus);

        if (paymentStatus == 1 || paymentStatus == 2) {
             order.setPaymentDate(LocalDateTime.now());
        }

        SupplyOrder savedOrder = supplyOrderRepository.save(order);

        // Audit Log quản lý trạng thái thanh toán khách hàng
        auditLogService.logAction(
            "UPDATE_PAYMENT",
            "SupplyOrder",
            orderId,
            String.format("Thay đổi payment status từ %d sang %d. CustomerId: %s", oldStatus, paymentStatus, order.getRestaurant().getId()),
            request
        );

        return savedOrder;
    }
}
