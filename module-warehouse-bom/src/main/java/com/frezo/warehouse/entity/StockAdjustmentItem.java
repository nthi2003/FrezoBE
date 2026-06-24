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
@Table(name = "stock_adjustment_items")
public class StockAdjustmentItem extends BaseEntity {

    @Column(name = "adjustment_id", nullable = false)
    private String adjustmentId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "expected_qty")
    private Double expectedQty;

    @Column(name = "actual_qty")
    private Double actualQty;

    @Column(name = "diff_qty")
    private Double diffQty;

    @Column(name = "unit_cost")
    private Double unitCost;

    @Column(name = "total_diff_value")
    private Double totalDiffValue;

    @Column(name = "reason", length = 500)
    private String reason;
}
