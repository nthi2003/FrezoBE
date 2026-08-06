package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.OkrCheckInRequest;
import com.frezo.qlns.dto.request.OkrRequest;
import com.frezo.qlns.dto.response.OkrListResponse;
import com.frezo.qlns.dto.response.OkrResponse;

import java.util.List;

public interface OkrService {
    OkrListResponse list(String scope, String ownerPersonId);
    OkrResponse get(String id);
    OkrResponse create(OkrRequest req);
    OkrResponse update(String id, OkrRequest req);
    void delete(String id);
    OkrResponse checkIn(String id, OkrCheckInRequest req);
    OkrResponse publish(String id);
}
