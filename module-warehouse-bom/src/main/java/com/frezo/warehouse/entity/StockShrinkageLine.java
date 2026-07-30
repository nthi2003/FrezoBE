package com.frezo.warehouse.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "stock_shrinkage_line")
public class StockShrinkageLine extends BaseEntity {

    @Column(name = "shrinkage_id", nullable = false, length = 36)
    private String shrinkageId;

    @Column(name = "batch_id", nullable = false, length = 36)
    private String batchId;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    /** SHRINK / DAMAGE / EXPIRED */
    @Column(name = "reason", nullable = false, length = 20)
    private String reason;

    @Column(name = "qty", nullable = false)
    private Double qty;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
