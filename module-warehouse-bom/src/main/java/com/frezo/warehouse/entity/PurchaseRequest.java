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
@Table(name = "purchase_request", indexes = {
        @Index(name = "idx_pr_status", columnList = "status"),
        @Index(name = "idx_pr_supplier", columnList = "supplier_id")
})
public class PurchaseRequest extends BaseEntity {

    @Column(name = "code", length = 50)
    private String code;

    @Column(name = "supplier_id", length = 36)
    private String supplierId;

    @Column(name = "warehouse_id", length = 36)
    private String warehouseId;

    /** DRAFT / PENDING / APPROVED / REJECTED / CANCELLED */
    @Column(name = "status", length = 30, nullable = false)
    private String status;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "approval_request_id", length = 36)
    private String approvalRequestId;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
}
