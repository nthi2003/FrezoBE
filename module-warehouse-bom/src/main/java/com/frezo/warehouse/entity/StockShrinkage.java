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

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "stock_shrinkage")
public class StockShrinkage extends BaseEntity {

    @Column(name = "shrinkage_code", nullable = false, length = 50)
    private String shrinkageCode;

    @Column(name = "warehouse_id", nullable = false, length = 36)
    private String warehouseId;

    /** DRAFT / CONFIRMED / CANCELLED */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
}
