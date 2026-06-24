package com.frezo.warehouse.service;

import com.frezo.warehouse.dto.request.WarehouseZoneRequest;
import com.frezo.warehouse.dto.response.WarehouseZoneResponse;

import java.util.List;

public interface WarehouseZoneService {
    WarehouseZoneResponse getById(String id);
    List<WarehouseZoneResponse> getByWarehouseId(String warehouseId);
    WarehouseZoneResponse create(WarehouseZoneRequest request);
    WarehouseZoneResponse update(String id, WarehouseZoneRequest request);
    void delete(String id);
}
