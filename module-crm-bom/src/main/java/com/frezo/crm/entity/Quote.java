package com.frezo.crm.entity;

import com.frezo.common.domain.BaseEntity;
import com.frezo.crm.common.QuoteStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "crm_quote",
        uniqueConstraints = @UniqueConstraint(name = "uk_crm_quote_code", columnNames = {"code"}),
        indexes = {
                @Index(name = "idx_crm_quote_deal", columnList = "deal_id"),
                @Index(name = "idx_crm_quote_customer", columnList = "customer_id")
        })
public class Quote extends BaseEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @Column(name = "deal_id", length = 36)
    private String dealId;

    @Column(name = "customer_id", length = 36)
    private String customerId;

    @Column(name = "issued_date", nullable = false)
    private LocalDate issuedDate;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "subtotal", precision = 20, scale = 2, nullable = false)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", precision = 20, scale = 2, nullable = false)
    private BigDecimal taxAmount;

    @Column(name = "discount_amount", precision = 20, scale = 2, nullable = false)
    private BigDecimal discountAmount;

    @Column(name = "total", precision = 20, scale = 2, nullable = false)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuoteStatus status;

    @Column(length = 2000)
    private String notes;
}
