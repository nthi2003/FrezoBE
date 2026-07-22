package com.frezo.warehouse.service;

import com.frezo.common.response.FePage;
import com.frezo.warehouse.dto.request.FromAlertsRequest;
import com.frezo.warehouse.dto.request.PurchaseRequestSaveRequest;
import com.frezo.warehouse.dto.response.PurchaseRequestDto;

import java.util.List;

public interface PurchaseRequestService {

    List<PurchaseRequestDto> createFromAlerts(FromAlertsRequest req);

    FePage<PurchaseRequestDto> list();

    PurchaseRequestDto get(String id);

    PurchaseRequestDto create(PurchaseRequestSaveRequest req);

    PurchaseRequestDto update(String id, PurchaseRequestSaveRequest req);

    void delete(String id);

    PurchaseRequestDto submit(String id);
}
