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

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "purchase_order", indexes = {
        @Index(name = "idx_po_pr", columnList = "pr_id"),
        @Index(name = "idx_po_status", columnList = "status")
})
public class PurchaseOrder extends BaseEntity {

    @Column(name = "code", length = 50)
    private String code;

    /** PurchaseRequest nguồn — idempotent 1 PR → 1 PO. */
    @Column(name = "pr_id", length = 36)
    private String prId;

    @Column(name = "supplier_id", length = 36)
    private String supplierId;

    @Column(name = "warehouse_id", length = 36)
    private String warehouseId;

    /** DRAFT / CONFIRMED / RECEIVED / CANCELLED */
    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;
}
