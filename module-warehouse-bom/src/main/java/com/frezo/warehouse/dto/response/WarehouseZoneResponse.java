package com.frezo.warehouse.dto.response;

import lombok.Data;

@Data
public class WarehouseZoneResponse {
    private String id;
    private String warehouseId;
    private String code;
    private String name;
    private String type;
    private String status;
}
