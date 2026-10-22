package com.frezo.qtbv.service;

import com.frezo.qtbv.dto.request.AssetAssignRequest;
import com.frezo.qtbv.dto.request.AssetSaveRequest;
import com.frezo.qtbv.dto.request.AssetTransferCreateRequest;
import com.frezo.qtbv.dto.response.AssetAssignmentResponse;
import com.frezo.qtbv.dto.response.AssetResponse;
import com.frezo.qtbv.dto.response.AssetTransferRequestResponse;

import java.util.List;
import java.util.Map;

public interface AssetService {

    Map<String, Object> list(String keyword, String status, String categoryCode, String assignedPersonId,
                             int page, int size);

    AssetResponse getById(String id);

    AssetResponse create(AssetSaveRequest request);

    AssetResponse update(String id, AssetSaveRequest request);

    void delete(String id);


    AssetResponse assign(String id, AssetAssignRequest request);

    AssetResponse unassign(String id, String note);

    AssetResponse startMaintenance(String id, String note);

    AssetResponse endMaintenance(String id, String note, java.math.BigDecimal cost);

    AssetResponse dispose(String id, String note);

    List<AssetAssignmentResponse> history(String assetId);

    Map<String, Object> stats();

    AssetTransferRequestResponse createTransferRequest(String assetId, AssetTransferCreateRequest req);


    AssetTransferRequestResponse approveTransferRequest(String requestId, String note);


    AssetTransferRequestResponse rejectTransferRequest(String requestId, String reason);


    AssetTransferRequestResponse cancelTransferRequest(String requestId);


    AssetTransferRequestResponse handoverTransferRequest(String requestId, String note);


    Map<String, Object> listTransferRequests(String status, String requestType, String assetId,
                                             String personId, String keyword, int page, int size);


    AssetTransferRequestResponse getTransferRequest(String requestId);
}
