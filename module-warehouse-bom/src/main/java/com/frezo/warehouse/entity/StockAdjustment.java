package com.frezo.warehouse.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "stock_adjustments")
public class StockAdjustment extends BaseEntity {

    @Column(name = "adjustment_code", unique = true, nullable = false, length = 50)
    private String adjustmentCode;

    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;

    @Column(name = "status", length = 20)
    private String status; // DRAFT, CONFIRMED, CANCELLED

    @Column(name = "total_diff_value")
    private Double totalDiffValue;

    @Column(name = "adjusted_by", length = 100)
    private String adjustedBy;

    @Column(name = "adjusted_at")
    private LocalDateTime adjustedAt;

    @Column(name = "note", length = 2000)
    private String note;
}
