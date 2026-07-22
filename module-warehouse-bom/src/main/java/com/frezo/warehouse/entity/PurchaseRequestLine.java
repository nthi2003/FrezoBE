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

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "purchase_request_line", indexes = {
        @Index(name = "idx_pr_line_pr", columnList = "purchase_request_id")
})
public class PurchaseRequestLine extends BaseEntity {

    @Column(name = "purchase_request_id", length = 36, nullable = false)
    private String purchaseRequestId;

    @Column(name = "product_id", length = 36, nullable = false)
    private String productId;

    @Column(name = "warehouse_id", length = 36)
    private String warehouseId;

    @Column(name = "qty", nullable = false)
    private Double qty;

    @Column(name = "stock_alert_id", length = 36)
    private String stockAlertId;

    @Column(name = "note", length = 500)
    private String note;
}
