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
@Table(name = "goods_receipt_notes")
public class GoodsReceiptNote extends BaseEntity {

    @Column(name = "grn_code", unique = true, nullable = false, length = 50)
    private String grnCode;

    @Column(name = "purchase_order_id")
    private String purchaseOrderId;

    @Column(name = "warehouse_id")
    private String warehouseId;

    @Column(name = "supplier_id")
    private String supplierId; // NCC_id

    @Column(name = "status", length = 20)
    private String status; // DRAFT, CONFIRMED, CANCELLED

    @Column(name = "total_value")
    private Double totalValue;

    @Column(name = "received_by")
    private String receivedBy;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "note", length = 2000)
    private String note;
}
