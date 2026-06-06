package com.frezo.product.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_batches")
public class Batch extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "supplier_id")
    private String supplierId;

    @Column(name = "batch_code", unique = true, nullable = false)
    private String batchCode; // QR code or batch reference

    @Column(name = "growing_area")
    private String growingArea; // Vùng trồng (Đơn Dương, Đức Trọng...)

    @Column(name = "import_date")
    private LocalDate importDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "initial_quantity")
    private Double initialQuantity;

    @Column(name = "current_quantity")
    private Double currentQuantity;

    @Column(name = "cost_price")
    private Double costPrice; // Giá vốn nhập

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;
}
