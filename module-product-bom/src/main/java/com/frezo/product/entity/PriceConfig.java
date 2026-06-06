package com.frezo.product.entity;

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
@Table(name = "price_configs")
public class PriceConfig extends BaseEntity {

    @Column(name = "product_unit_id", nullable = false)
    private String productUnitId;

    @Column(name = "price_group_id", nullable = false)
    private String priceGroupId;

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "effective_date")
    private LocalDateTime effectiveDate; // Ngày bắt đầu áp dụng

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate; // Ngày hết hạn (cho Đặt lịch giá trước)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_unit_id", insertable = false, updatable = false)
    private ProductUnit productUnit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_group_id", insertable = false, updatable = false)
    private PriceGroup priceGroup;
}
