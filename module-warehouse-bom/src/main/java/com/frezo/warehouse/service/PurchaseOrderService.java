package com.frezo.warehouse.service;

import com.frezo.common.response.FePage;
import com.frezo.warehouse.dto.request.PurchaseOrderSaveRequest;
import com.frezo.warehouse.dto.response.PurchaseOrderDto;

public interface PurchaseOrderService {

    FePage<PurchaseOrderDto> list();

    PurchaseOrderDto get(String id);

    PurchaseOrderDto create(PurchaseOrderSaveRequest req);

    PurchaseOrderDto update(String id, PurchaseOrderSaveRequest req);

    void delete(String id);

    /** Tạo PO từ PR APPROVED — idempotent theo prId. */
    PurchaseOrderDto createFromPr(String prId);

    /** Xác nhận nhận hàng + stub tăng StockBalance. */
    PurchaseOrderDto confirmReceive(String id);
}
