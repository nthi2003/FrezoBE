package com.frezo.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductWarehouseContextDto {
    private String productId;
    private Double totalOnHand;
    private Double totalAvailable;
    private Double totalReserved;
    private List<ProductStockChartPoint> stockChart;
    private List<ProductStockLevelDto> stockLevels;
    private List<ReorderRuleDto> reorderRules;
}
