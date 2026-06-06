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
@Table(name = "market_prices")
public class MarketPrice extends BaseEntity {

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "market_name", length = 100)
    private String marketName; // Chợ đầu mối Thủ Đức, Bình Điền...

    @Column(name = "price", nullable = false)
    private Double price;

    @Column(name = "unit", length = 20)
    private String unit; // kg, bó, rổ...

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(name = "note")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;
}
