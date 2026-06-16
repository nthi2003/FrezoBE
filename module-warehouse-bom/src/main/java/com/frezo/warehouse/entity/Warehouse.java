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
@Table(name = "warehouses")
public class Warehouse extends BaseEntity {

    @Column(name = "code", unique = true, nullable = false, length = 20)
    private String code;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "short_name", length = 50)
    private String shortName;

    @Column(name = "type", length = 20)
    private String type; // MAIN, TRANSIT, RETURNS, VIRTUAL

    @Column(name = "address_line", length = 300)
    private String addressLine;

    @Column(name = "ward", length = 100)
    private String ward;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "province", length = 100)
    private String province;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "manager_id")
    private String managerId;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "total_area_sqm")
    private Double totalAreaSqm;

    @Column(name = "max_capacity")
    private Double maxCapacity;

    @Column(name = "capacity_unit", length = 10)
    private String capacityUnit; // PALLET, CBM, UNIT

    @Column(name = "temperature_min")
    private Double temperatureMin;

    @Column(name = "temperature_max")
    private Double temperatureMax;

    @Column(name = "is_cold_storage")
    private Boolean isColdStorage = false;

    @Column(name = "status", length = 20)
    private String status; // ACTIVE, INACTIVE, CLOSED

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "note", length = 2000)
    private String note;
}
