package com.frezo.warehouse.dto.response;

import lombok.Data;

@Data
public class StockBalanceResponse {
    private String id;
    private String productId;
    private String warehouseId;
    private String locationId;
    private String batchId;
    private Double quantityOnHand;
    private Double quantityReserved;
    private Double quantityAvailable;
}
