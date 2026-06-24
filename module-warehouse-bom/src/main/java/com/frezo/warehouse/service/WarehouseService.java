package com.frezo.warehouse.service;

import com.frezo.common.response.PageResponse;
import com.frezo.warehouse.dto.request.WarehouseCreateRequest;
import com.frezo.warehouse.dto.request.WarehouseUpdateRequest;
import com.frezo.warehouse.dto.response.WarehouseResponse;

import java.util.List;

public interface WarehouseService {
    WarehouseResponse getById(String id);
    WarehouseResponse getByCode(String code);
    List<WarehouseResponse> getAll();
    PageResponse<WarehouseResponse> filter(String keyword, String status, int page, int size);
    WarehouseResponse create(WarehouseCreateRequest request);
    WarehouseResponse update(String id, WarehouseUpdateRequest request);
    void delete(String id);
}
