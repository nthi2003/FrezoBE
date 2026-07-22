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

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "purchase_order_line", indexes = {
        @Index(name = "idx_po_line_po", columnList = "purchase_order_id")
})
public class PurchaseOrderLine extends BaseEntity {

    @Column(name = "purchase_order_id", length = 36, nullable = false)
    private String purchaseOrderId;

    @Column(name = "pr_line_id", length = 36)
    private String prLineId;

    @Column(name = "product_id", length = 36, nullable = false)
    private String productId;

    @Column(name = "warehouse_id", length = 36)
    private String warehouseId;

    @Column(name = "qty", nullable = false)
    private Double qty;

    @Column(name = "received_qty")
    private Double receivedQty;

    @Column(name = "note", length = 500)
    private String note;
}
