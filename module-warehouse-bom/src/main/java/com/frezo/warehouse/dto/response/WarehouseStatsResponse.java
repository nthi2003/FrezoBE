package com.frezo.warehouse.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WarehouseStatsResponse {
    private long totalWarehouses;
    private long totalProductsInStock;
    private long lowStockAlerts;
    private long grnToday;
    private long ginToday;
    private long totalStockValue;
}
