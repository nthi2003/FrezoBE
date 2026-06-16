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
@Table(name = "stock_ledger")
public class StockLedger extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "batch_id")
    private String batchId;

    @Column(name = "warehouse_id")
    private String warehouseId;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType; // IN, OUT, ADJUSTMENT

    @Column(name = "quantity", nullable = false)
    private Double quantity;

    @Column(name = "unit_cost")
    private Double unitCost;

    @Column(name = "total_value")
    private Double totalValue;

    @Column(name = "reference_type", length = 20)
    private String referenceType; // GRN, SO, ADJ

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "note", length = 500)
    private String note;
}
