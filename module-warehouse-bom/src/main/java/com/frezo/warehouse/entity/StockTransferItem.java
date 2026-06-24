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
@Table(name = "stock_transfer_items")
public class StockTransferItem extends BaseEntity {

    @Column(name = "transfer_id", nullable = false)
    private String transferId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "qty_transferred")
    private Double qtyTransferred;

    @Column(name = "unit_cost")
    private Double unitCost;

    @Column(name = "from_location_id")
    private String fromLocationId;

    @Column(name = "to_location_id")
    private String toLocationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_id", insertable = false, updatable = false)
    private StockTransfer stockTransfer;
}
