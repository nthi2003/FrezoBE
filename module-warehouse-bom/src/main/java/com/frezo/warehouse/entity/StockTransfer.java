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
@Table(name = "stock_transfers")
public class StockTransfer extends BaseEntity {

    @Column(name = "transfer_code", unique = true, nullable = false, length = 50)
    private String transferCode;

    @Column(name = "from_warehouse_id", nullable = false)
    private String fromWarehouseId;

    @Column(name = "to_warehouse_id", nullable = false)
    private String toWarehouseId;

    @Column(name = "status", length = 20)
    private String status; // DRAFT, CONFIRMED, CANCELLED

    @Column(name = "total_value")
    private Double totalValue;

    @Column(name = "transferred_by")
    private String transferredBy;

    @Column(name = "transferred_at")
    private LocalDateTime transferredAt;

    @Column(name = "note", length = 2000)
    private String note;
}
