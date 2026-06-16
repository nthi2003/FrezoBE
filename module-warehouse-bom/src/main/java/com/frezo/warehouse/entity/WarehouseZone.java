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
@Table(name = "warehouse_zones")
public class WarehouseZone extends BaseEntity {

    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;

    @Column(name = "code", length = 20)
    private String code;

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "type", length = 20)
    private String type; // STORAGE, PICKING, STAGING, QUARANTINE

    @Column(name = "status", length = 20)
    private String status; // ACTIVE, INACTIVE

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", insertable = false, updatable = false)
    private Warehouse warehouse;
}
