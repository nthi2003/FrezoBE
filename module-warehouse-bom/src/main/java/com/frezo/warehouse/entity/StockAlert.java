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
@Table(name = "stock_alert", indexes = {
        @Index(name = "idx_stock_alert_status", columnList = "status"),
        @Index(name = "idx_stock_alert_product_wh", columnList = "product_id,warehouse_id")
})
public class StockAlert extends BaseEntity {

    @Column(name = "warehouse_id", length = 36, nullable = false)
    private String warehouseId;

    @Column(name = "product_id", length = 36, nullable = false)
    private String productId;

    @Column(name = "current_qty")
    private Double currentQty;

    @Column(name = "min_qty")
    private Double minQty;

    /** CRITICAL / WARNING / INFO */
    @Column(name = "severity", length = 20)
    private String severity;

    /** OPEN / DISMISSED / RESOLVED */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "triggered_at")
    private LocalDateTime triggeredAt;

    @Column(name = "dismissed_at")
    private LocalDateTime dismissedAt;

    /** Idempotency key: productId|warehouseId|yyyyMMdd */
    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    /** PR đang active gắn alert (1 alert ↔ 1 PR active). */
    @Column(name = "purchase_request_id", length = 36)
    private String purchaseRequestId;
}
