package com.frezo.warehouse.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_balances", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"product_id", "warehouse_id", "location_id", "batch_id"})
})
public class StockBalance extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "quantity_on_hand")
    private Double quantityOnHand = 0.0;

    @Column(name = "quantity_reserved")
    private Double quantityReserved = 0.0;

    @Column(name = "quantity_available")
    private Double quantityAvailable = 0.0;
}
