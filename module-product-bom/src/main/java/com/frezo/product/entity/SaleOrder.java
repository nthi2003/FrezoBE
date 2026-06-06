// SaleOrder.java
package com.frezo.product.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sale_orders")
public class SaleOrder extends BaseEntity {

    @Column(name = "cart_id")
    private String cartId;

    @Column(name = "staff_id")
    private String staffId;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "order_code", unique = true, nullable = false)
    private String orderCode;

    @Column(name = "total_amount", nullable = false)
    private Double totalAmount;

    @Column(name = "payment_status", length = 20)
    private String paymentStatus; // PENDING, PAID, FAILED, CANCELLED

    @Column(name = "payment_method", length = 20)
    private String paymentMethod; // VIETQR, CASH

    @Column(name = "sepay_order_id")
    private String sepayOrderId;

    @Column(name = "qr_content", length = 1000)
    private String qrContent;

    @Column(name = "qr_image_url")
    private String qrImageUrl;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;
}