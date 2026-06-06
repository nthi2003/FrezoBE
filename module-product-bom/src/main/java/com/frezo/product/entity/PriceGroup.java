package com.frezo.product.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "price_groups")
public class PriceGroup extends BaseEntity {

    @Column(name = "code", unique = true, nullable = false)
    private String code; // RETAIL, AGENT, RESTAURANT, SUPERMARKET...

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
