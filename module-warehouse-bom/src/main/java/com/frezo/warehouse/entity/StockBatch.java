package com.frezo.warehouse.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "stock_batch", indexes = {
        @Index(name = "idx_stock_batch_product_wh", columnList = "product_id,warehouse_id")
})
public class StockBatch extends BaseEntity {

    @Column(name = "batch_code", nullable = false, length = 100)
    private String batchCode;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    @Column(name = "supplier_id", length = 36)
    private String supplierId;

    @Column(name = "grn_id", length = 36)
    private String grnId;

    @Column(name = "grn_item_id", length = 36)
    private String grnItemId;

    @Column(name = "warehouse_location_id", length = 36)
    private String warehouseLocationId;

    @Column(name = "received_date")
    private LocalDate receivedDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "qty_on_hand")
    private Double qtyOnHand;

    /** ACTIVE / DEPLETED / EXPIRED */
    @Column(name = "status", length = 20)
    private String status;
}
