// PaymentTransaction.java
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
@Table(name = "payment_transactions")
public class PaymentTransaction extends BaseEntity {

    @Column(name = "order_id", nullable = false)
    private String orderId;

    @Column(name = "transaction_code")
    private String transactionCode;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "gateway", length = 20)
    private String gateway; // SEPAY

    @Column(name = "raw_webhook", columnDefinition = "TEXT")
    private String rawWebhook; // lưu nguyên json để debug

    @Column(name = "received_at")
    private LocalDateTime receivedAt;
}