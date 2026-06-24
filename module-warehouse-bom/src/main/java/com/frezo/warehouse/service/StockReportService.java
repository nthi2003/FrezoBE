package com.frezo.warehouse.service;

import com.frezo.warehouse.dto.response.LowStockAlertResponse;
import com.frezo.warehouse.dto.response.StockMovementReportResponse;
import com.frezo.warehouse.dto.response.StockMovementResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface StockReportService {
    List<StockMovementReportResponse> getStockMovementReport(
            LocalDateTime from, LocalDateTime to, String productId, String warehouseId);
    byte[] exportStockMovementExcel(
            LocalDateTime from, LocalDateTime to, String productId, String warehouseId);
    List<LowStockAlertResponse> getLowStockAlerts();
}
