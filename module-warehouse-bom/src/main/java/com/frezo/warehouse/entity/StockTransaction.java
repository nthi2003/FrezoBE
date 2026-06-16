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
@Table(name = "stock_transactions")
public class StockTransaction extends BaseEntity {

    @Column(name = "transaction_code", unique = true, nullable = false, length = 50)
    private String transactionCode;

    @Column(name = "transaction_type", nullable = false, length = 20)
    private String transactionType; // RECEIPT, ISSUE, TRANSFER, ADJUSTMENT

    @Column(name = "reference_type", length = 20)
    private String referenceType; // GRN, SO, ADJ

    @Column(name = "reference_id")
    private String referenceId;

    @Column(name = "warehouse_id")
    private String warehouseId;

    @Column(name = "total_amount")
    private Double totalAmount;

    @Column(name = "status", length = 20)
    private String status; // DRAFT, CONFIRMED, CANCELLED

    @Column(name = "note", length = 2000)
    private String note;
}
