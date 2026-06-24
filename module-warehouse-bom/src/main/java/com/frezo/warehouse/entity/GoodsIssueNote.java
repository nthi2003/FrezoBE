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

    @Column(name = "issue_type", length = 20)
    private String issueType; // SALES, INTERNAL_TRANSFER, DAMAGE_RETURN, ADJUSTMENT

    @Column(name = "status", length = 20)
    private String status; // DRAFT, CONFIRMED, CANCELLED

    @Column(name = "total_value")
    private Double totalValue;

    @Column(name = "issued_by")
    private String issuedBy;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "note", length = 2000)
    private String note;
}
