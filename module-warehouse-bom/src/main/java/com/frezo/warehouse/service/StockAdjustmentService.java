package com.frezo.warehouse.service;

import com.frezo.common.response.PageResponse;
import com.frezo.warehouse.dto.request.StockAdjustmentCreateRequest;
import com.frezo.warehouse.dto.response.StockAdjustmentResponse;

import java.util.List;

public interface StockAdjustmentService {
    StockAdjustmentResponse create(StockAdjustmentCreateRequest request);
    StockAdjustmentResponse confirm(String id);
    void cancel(String id, String reason);
    StockAdjustmentResponse getById(String id);
    StockAdjustmentResponse getByCode(String code);
    PageResponse<StockAdjustmentResponse> filter(String status, String warehouseId, int page, int size);
    void delete(String id);
}
