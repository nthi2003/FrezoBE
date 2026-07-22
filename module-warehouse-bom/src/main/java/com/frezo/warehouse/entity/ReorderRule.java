package com.frezo.warehouse.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
@Table(name = "reorder_rule",
        uniqueConstraints = @UniqueConstraint(name = "uk_reorder_product_wh",
                columnNames = {"product_id", "warehouse_id"}),
        indexes = @Index(name = "idx_reorder_active", columnList = "active"))
public class ReorderRule extends BaseEntity {

    @Column(name = "warehouse_id", length = 36, nullable = false)
    private String warehouseId;

    @Column(name = "product_id", length = 36, nullable = false)
    private String productId;

    @Column(name = "min_qty", nullable = false)
    private Double minQty;

    @Column(name = "max_qty", nullable = false)
    private Double maxQty;

    @Column(name = "reorder_qty")
    private Double reorderQty;

    /** NCC ưu tiên khi sinh PurchaseRequest từ alert. */
    @Column(name = "preferred_supplier_id", length = 36)
    private String preferredSupplierId;

    @Column(name = "active")
    private Boolean active;
}
