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
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "stock_take", indexes = @Index(name = "idx_stock_take_wh", columnList = "warehouse_id"))
public class StockTake extends BaseEntity {

    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "warehouse_id", length = 36, nullable = false)
    private String warehouseId;

    @Column(name = "take_date")
    private LocalDate takeDate;

    /** DRAFT / IN_PROGRESS / SUBMITTED / POSTED / CANCELLED */
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "posted_at")
    private LocalDateTime postedAt;
}
