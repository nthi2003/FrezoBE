package com.frezo.warehouse.dto.response;

import lombok.Data;

@Data
public class LowStockAlertResponse {
    private String productId;
    private String warehouseId;
    private String locationId;
    private String batchId;
    private Double quantityAvailable;
    private Double warningThreshold;
}
