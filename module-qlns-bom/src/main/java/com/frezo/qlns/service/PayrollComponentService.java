package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.PayrollComponentRequest;
import com.frezo.qlns.dto.response.PayrollComponentResponse;

import java.util.List;

public interface PayrollComponentService {
    List<PayrollComponentResponse> list();

    PayrollComponentResponse getById(String id);

    PayrollComponentResponse create(PayrollComponentRequest request);

    PayrollComponentResponse update(String id, PayrollComponentRequest request);

    void delete(String id);
}
