package com.frezo.warehouse.service;

import com.frezo.warehouse.dto.request.WarehouseLocationRequest;
import com.frezo.warehouse.dto.response.WarehouseLocationResponse;

import java.util.List;

public interface WarehouseLocationService {
    WarehouseLocationResponse getById(String id);
    List<WarehouseLocationResponse> getByZoneId(String zoneId);
    WarehouseLocationResponse getByBarcode(String barcode);
    WarehouseLocationResponse create(WarehouseLocationRequest request);
    WarehouseLocationResponse update(String id, WarehouseLocationRequest request);
    void delete(String id);
}
