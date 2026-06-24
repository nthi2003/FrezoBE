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
@Table(name = "goods_issue_note_items")
public class GoodsIssueNoteItem extends BaseEntity {

    @Column(name = "gin_id", nullable = false)
    private String ginId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "qty_requested")
    private Double qtyRequested;

    @Column(name = "qty_issued")
    private Double qtyIssued;

    @Column(name = "unit_cost")
    private Double unitCost;

    @Column(name = "location_id")
    private String locationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gin_id", insertable = false, updatable = false)
    private GoodsIssueNote goodsIssueNote;
}
