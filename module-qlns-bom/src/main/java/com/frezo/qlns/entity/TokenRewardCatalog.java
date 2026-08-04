package com.frezo.qlns.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Catalog thưởng tùy chọn — MVP seed tối thiểu, shop UI = phase sau. */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "token_reward_catalog")
public class TokenRewardCatalog extends BaseEntity {

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "token_cost", nullable = false, precision = 18, scale = 2)
    private BigDecimal tokenCost;

    @Column(name = "cash_value", precision = 18, scale = 2)
    private BigDecimal cashValue;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "description", length = 1000)
    private String description;
}
