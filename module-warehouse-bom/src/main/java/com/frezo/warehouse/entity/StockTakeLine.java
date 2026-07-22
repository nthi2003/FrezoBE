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
@Table(name = "stock_take_line", indexes = @Index(name = "idx_stl_take", columnList = "stock_take_id"))
public class StockTakeLine extends BaseEntity {

    @Column(name = "stock_take_id", length = 36, nullable = false)
    private String stockTakeId;

    @Column(name = "product_id", length = 36, nullable = false)
    private String productId;

    @Column(name = "system_qty")
    private Double systemQty;

    @Column(name = "counted_qty")
    private Double countedQty;

    @Column(name = "variance_qty")
    private Double varianceQty;

    @Column(name = "note", length = 500)
    private String note;
}
