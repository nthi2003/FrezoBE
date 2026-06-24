package com.frezo.warehouse.service;

import com.frezo.common.response.PageResponse;
import com.frezo.warehouse.dto.request.TransferConfirmRequest;
import com.frezo.warehouse.dto.request.TransferCreateRequest;
import com.frezo.warehouse.dto.response.TransferResponse;

public interface StockTransferService {
    TransferResponse getById(String id);
    TransferResponse getByCode(String transferCode);
    PageResponse<TransferResponse> filter(String status, String keyword, int page, int size);
    TransferResponse create(TransferCreateRequest request);
    TransferResponse confirm(String id, TransferConfirmRequest request);
    void cancel(String id, String reason);
    void delete(String id);
}
