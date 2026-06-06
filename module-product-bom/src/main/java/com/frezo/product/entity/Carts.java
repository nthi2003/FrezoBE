package com.frezo.product.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "carts")
public class Carts extends BaseEntity {
    @Column(name = "staff_id", nullable = false)
    private String staffId;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "status", length = 20)
    private String status; // OPEN, CHECKED_OUT, CANCELLED

    @Column(name = "note")
    private String note;
}
