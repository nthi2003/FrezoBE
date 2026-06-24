package com.frezo.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class WarehouseZoneRequest {
    @NotBlank(message = "Kho không được để trống")
    private String warehouseId;

    private String code;
    private String name;
    private String type;
    private String status;
}
