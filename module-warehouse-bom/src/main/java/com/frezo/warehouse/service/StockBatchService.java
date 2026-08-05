package com.frezo.warehouse.service;

import com.frezo.common.response.FePage;
import com.frezo.warehouse.dto.response.FefoSuggestResponse;
import com.frezo.warehouse.dto.response.StockBatchResponse;

import java.time.LocalDate;

public interface StockBatchService {

    StockBatchResponse getById(String id);

    FePage<StockBatchResponse> list(
            String warehouseId,
            String productId,
            String status,
            String keyword,
            LocalDate expiryFrom,
            LocalDate expiryTo);

    FefoSuggestResponse suggestFefo(String warehouseId, String productId, double qty);

    /** Tính HSD dự kiến theo loại SP (rau lá +3d, củ quả +7d). */
    LocalDate computeExpiryDate(String productId, LocalDate receivedDate);

    String generateBatchCode(String productId, String supplierId, LocalDate receivedDate);
}
