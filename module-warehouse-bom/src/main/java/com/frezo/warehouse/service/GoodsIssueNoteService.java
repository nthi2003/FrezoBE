package com.frezo.warehouse.service;

import com.frezo.common.response.PageResponse;
import com.frezo.warehouse.dto.request.GinConfirmRequest;
import com.frezo.warehouse.dto.request.GinCreateRequest;
import com.frezo.warehouse.dto.response.GinResponse;

import java.util.List;

public interface GoodsIssueNoteService {
    GinResponse getById(String id);
    GinResponse getByCode(String ginCode);
    PageResponse<GinResponse> filter(String status, String keyword, int page, int size);
    GinResponse create(GinCreateRequest request);
    GinResponse confirm(String id, GinConfirmRequest request);
    void batchConfirm(List<String> ids);
    void cancel(String id, String reason);
    void batchCancel(List<String> ids, String reason);
    void delete(String id);
}
