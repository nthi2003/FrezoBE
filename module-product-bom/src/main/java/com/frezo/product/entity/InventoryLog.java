package com.frezo.product.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "inventory_logs")
public class InventoryLog extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "type", length = 20)
    private String type; // IMPORT, EXPORT, SHRINKAGE (Hao hụt/Hỏng)

    @Column(name = "quantity", nullable = false)
    private Double quantity;

    @Column(name = "note")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", insertable = false, updatable = false)
    private Batch batch;
}
