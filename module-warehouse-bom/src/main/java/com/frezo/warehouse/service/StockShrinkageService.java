package com.frezo.warehouse.service;

import com.frezo.common.response.FePage;
import com.frezo.warehouse.dto.request.ShrinkageCreateRequest;
import com.frezo.warehouse.dto.response.ShrinkageResponse;

public interface StockShrinkageService {

    ShrinkageResponse create(ShrinkageCreateRequest request);

    ShrinkageResponse confirm(String id);

    void cancel(String id, String reason);

    ShrinkageResponse getById(String id);

    FePage<ShrinkageResponse> list(String warehouseId, String status);
}
