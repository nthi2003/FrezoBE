package com.frezo.product.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_units")
public class ProductUnit extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "unit_name", nullable = false)
    private String unitName; // kg, bó, thùng, rổ...

    @Column(name = "conversion_rate")
    private Double conversionRate; // Tỷ lệ quy đổi (ví dụ 1 thùng = 10kg thì rate = 10)

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;
}
