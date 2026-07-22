package com.frezo.crm.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "crm_quote_item", indexes = @Index(name = "idx_crm_qi_quote", columnList = "quote_id"))
public class QuoteItem extends BaseEntity {

    @Column(name = "quote_id", nullable = false, length = 36)
    private String quoteId;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @Column(name = "product_id", length = 36)
    private String productId;

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(name = "unit_price", nullable = false, precision = 20, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "tax_rate", precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "discount_pct", precision = 5, scale = 2)
    private BigDecimal discountPct;

    @Column(name = "line_total", nullable = false, precision = 20, scale = 2)
    private BigDecimal lineTotal;

    @Column(length = 500)
    private String description;
}
