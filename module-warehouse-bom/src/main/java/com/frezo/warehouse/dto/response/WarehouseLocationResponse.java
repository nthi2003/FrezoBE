package com.frezo.warehouse.dto.response;

import lombok.Data;

@Data
public class WarehouseLocationResponse {
    private String id;
    private String zoneId;
    private String aisle;
    private String rack;
    private String level;
    private String bin;
    private String barcode;
    private Double maxWeightKg;
    private Boolean isActive;
}
