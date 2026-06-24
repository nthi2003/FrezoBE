package com.frezo.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WarehouseLocationRequest {
    @NotBlank(message = "Zone không được để trống")
    private String zoneId;

    private String aisle;
    private String rack;
    private String level;
    private String bin;
    private String barcode;
    private Double maxWeightKg;
    private Boolean isActive;
}
