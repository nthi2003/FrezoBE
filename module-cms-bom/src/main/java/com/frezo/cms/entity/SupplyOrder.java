package com.frezo.cms.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "supply_orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplyOrder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @Column(name = "order_code", unique = true)
    private String orderCode;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "payment_status")
    private Integer paymentStatus; // 0: Pending, 1: Paid, 2: Partial

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;
}
