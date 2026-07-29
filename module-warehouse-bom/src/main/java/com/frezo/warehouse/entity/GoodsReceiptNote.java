package com.frezo.warehouse.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
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

    @Column(name = "status", length = 30)
    private String status; // DRAFT, PENDING_APPROVAL, APPROVED, CONFIRMED, CANCELLED

    /** Số hóa đơn GTGT đầu vào từ NCC (T3/AMIS). */
    @Column(name = "invoice_no", length = 50)
    private String invoiceNo;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "total_value")
    private Double totalValue;

    @Column(name = "received_by")
    private String receivedBy;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "note", length = 2000)
    private String note;
}
