package com.frezo.warehouse.service;

import com.frezo.common.response.PageResponse;
import com.frezo.warehouse.dto.response.LowStockAlertResponse;
import com.frezo.warehouse.dto.response.StockAvailableResponse;
import com.frezo.warehouse.dto.response.StockBalanceResponse;
import com.frezo.warehouse.dto.response.WarehouseStatsResponse;

import java.util.List;

public interface StockBalanceService {
    PageResponse<StockBalanceResponse> filter(String warehouseId, String productId, String keyword, int page, int size);
    StockBalanceResponse getById(String id);
    List<LowStockAlertResponse> getLowStockAlerts();
    WarehouseStatsResponse getStats();
    byte[] exportStock(String warehouseId, String productId);

    /**
     * Tồn khả dụng theo product + warehouse (+ batch tùy chọn).
     * Nếu {@code productIds} có nhiều id → trả về danh sách tương ứng.
     */
    List<StockAvailableResponse> getAvailable(String warehouseId, String productId, String productIds, String batchId);
}
