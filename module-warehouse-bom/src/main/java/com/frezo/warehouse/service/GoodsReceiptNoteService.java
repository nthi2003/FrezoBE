package com.frezo.warehouse.service;

import com.frezo.common.response.PageResponse;
import com.frezo.warehouse.dto.request.GrnConfirmRequest;
import com.frezo.warehouse.dto.request.GrnCreateRequest;
import com.frezo.warehouse.dto.request.GrnUpdateRequest;
import com.frezo.warehouse.dto.response.GrnResponse;

public interface GoodsReceiptNoteService {
    GrnResponse getById(String id);
    GrnResponse getByCode(String grnCode);
    PageResponse<GrnResponse> filter(String status, String keyword, int page, int size);
    GrnResponse create(GrnCreateRequest request);
    GrnResponse update(String id, GrnUpdateRequest request);
    GrnResponse submit(String id);
    GrnResponse approve(String id);
    GrnResponse confirm(String id, GrnConfirmRequest request);
    void cancel(String id, String reason);
    void delete(String id);
}
