package com.frezo.warehouse.service;

import com.frezo.warehouse.dto.request.StockTakeLineRequest;
import com.frezo.warehouse.dto.request.StockTakeRequest;
import com.frezo.warehouse.dto.response.StockTakeResponse;

import java.util.List;

public interface StockTakeService {
    List<StockTakeResponse> list(String warehouseId);
    StockTakeResponse get(String id);
    StockTakeResponse create(StockTakeRequest req);
    StockTakeResponse start(String id);
    StockTakeResponse submitCounted(String id, List<StockTakeLineRequest> lines);
    StockTakeResponse postVariance(String id);
}
