package com.frezo.warehouse.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "warehouse_locations")
public class WarehouseLocation extends BaseEntity {

    @Column(name = "zone_id", nullable = false)
    private String zoneId;

    @Column(name = "aisle", length = 10)
    private String aisle;

    @Column(name = "rack", length = 10)
    private String rack;

    @Column(name = "level", length = 10)
    private String level;

    @Column(name = "bin", length = 10)
    private String bin;

    @Column(name = "barcode", length = 50)
    private String barcode;

    @Column(name = "max_weight_kg")
    private Double maxWeightKg;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", insertable = false, updatable = false)
    private WarehouseZone zone;
}
