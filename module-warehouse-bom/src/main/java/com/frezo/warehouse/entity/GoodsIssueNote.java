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
@Table(name = "goods_issue_notes")
public class GoodsIssueNote extends BaseEntity {

    @Column(name = "gin_code", unique = true, nullable = false, length = 50)
    private String ginCode;

    @Column(name = "warehouse_id")
    private String warehouseId;

    @Column(name = "customer_id")
    private String customerId;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "issue_type", length = 30)
    private String issueType; // SALES, INTERNAL_TRANSFER, DAMAGE_RETURN, ADJUSTMENT

    @Column(name = "status", length = 30)
    private String status; // DRAFT, PENDING_APPROVAL, APPROVED, CONFIRMED, CANCELLED

    /** Số chứng từ / hóa đơn xuất (T3/AMIS). */
    @Column(name = "document_no", length = 50)
    private String documentNo;

    @Column(name = "document_date")
    private LocalDate documentDate;

    /** Kho đích khi issue_type = INTERNAL_TRANSFER. */
    @Column(name = "transfer_warehouse_id", length = 36)
    private String transferWarehouseId;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "total_value")
    private Double totalValue;

    @Column(name = "issued_by")
    private String issuedBy;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "note", length = 2000)
    private String note;
}
