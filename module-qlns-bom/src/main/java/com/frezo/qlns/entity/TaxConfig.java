package com.frezo.qlns.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "tax_config")
public class TaxConfig extends BaseEntity {

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "bracket_order", nullable = false)
    private Integer bracketOrder;

    @Column(name = "from_amount", precision = 15, scale = 2)
    private BigDecimal fromAmount;

    @Column(name = "to_amount", precision = 15, scale = 2)
    private BigDecimal toAmount;

    @Column(name = "rate", precision = 5, scale = 4)
    private BigDecimal rate;

    @Column(name = "deduct_amount", precision = 15, scale = 2)
    private BigDecimal deductAmount;

    @Column(name = "personal_deduction", precision = 15, scale = 2)
    private BigDecimal personalDeduction;

    @Column(name = "dependent_deduction", precision = 15, scale = 2)
    private BigDecimal dependentDeduction;

    @Column(name = "is_active")
    private Boolean isActive;
}
