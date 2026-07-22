package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.RequisitionRequest;
import com.frezo.qlns.dto.response.RequisitionResponse;

import java.util.List;

public interface RequisitionService {

    RequisitionResponse create(RequisitionRequest req);

    RequisitionResponse update(String id, RequisitionRequest req);

    RequisitionResponse getById(String id);

    List<RequisitionResponse> list(String status);

    RequisitionResponse close(String id);
}
