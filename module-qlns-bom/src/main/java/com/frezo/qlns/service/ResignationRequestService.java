package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.ResignationApproveRequest;
import com.frezo.qlns.dto.request.ResignationCreateRequest;
import com.frezo.qlns.dto.request.ResignationHandoverRequest;
import com.frezo.qlns.dto.response.ResignationResponse;

import java.util.List;

public interface ResignationRequestService {

    ResignationResponse create(ResignationCreateRequest request);

    ResignationResponse getById(String id);

    List<ResignationResponse> list(String personId, String status);

    ResignationResponse approve(String id, ResignationApproveRequest request);

    ResignationResponse confirmHandover(String id, ResignationHandoverRequest request);

    ResignationResponse settlePayroll(String id);

    ResignationResponse complete(String id);

    ResignationResponse cancel(String id);
}
