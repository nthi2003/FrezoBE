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
@Table(name = "goods_receipt_note_items")
public class GoodsReceiptNoteItem extends BaseEntity {

    @Column(name = "grn_id", nullable = false)
    private String grnId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "qty_expected")
    private Double qtyExpected;

    @Column(name = "qty_received")
    private Double qtyReceived;

    @Column(name = "unit_cost")
    private Double unitCost;

    @Column(name = "location_id")
    private String locationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grn_id", insertable = false, updatable = false)
    private GoodsReceiptNote goodsReceiptNote;
}
