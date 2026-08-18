package com.frezo.warehouse.service;

import com.frezo.warehouse.dto.response.ProductStockChartPoint;
import com.frezo.warehouse.dto.response.ProductStockLevelDto;
import com.frezo.warehouse.dto.response.ProductWarehouseContextDto;
import com.frezo.warehouse.dto.response.ReorderRuleDto;

import java.util.List;

public interface ProductWarehouseService {

    ProductWarehouseContextDto getContext(String productId, Integer days, String warehouseId);

    List<ProductStockChartPoint> getStockChart(String productId, Integer days, String warehouseId);

    List<ProductStockLevelDto> getStockLevels(String productId);

    List<ReorderRuleDto> getReorderRules(String productId);
}
