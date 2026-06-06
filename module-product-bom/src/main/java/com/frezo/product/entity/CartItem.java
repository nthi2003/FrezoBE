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
public class CartItem extends BaseEntity {

    @Column(name = "cart_id", nullable = false)
    private String cartId;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "product_unit_id", nullable = false)
    private String productUnitId;

    @Column(name = "price_group_code")
    private String priceGroupCode;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false)
    private Double unitPrice; // snapshot giá lúc thêm vào giỏ

    @Column(name = "total_price", nullable = false)
    private Double totalPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", insertable = false, updatable = false)
    private Carts cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_unit_id", insertable = false, updatable = false)
    private ProductUnit productUnit;
}
