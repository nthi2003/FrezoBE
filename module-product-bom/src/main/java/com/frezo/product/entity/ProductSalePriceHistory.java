package com.frezo.product.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

/**
 * Lịch sử thay đổi giá bán catalog ({@code products.price}).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_sale_price_history")
public class ProductSalePriceHistory extends BaseEntity {

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    /** Giá trước khi đổi; null khi thiết lập giá lần đầu. */
    @Column(name = "old_price")
    private Double oldPrice;

    @Column(name = "new_price", nullable = false)
    private Double newPrice;

    @Column(name = "note", length = 500)
    private String note;
}
