package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.PayrollPeriodRequest;
import com.frezo.qlns.dto.response.PayrollPeriodResponse;
import com.frezo.common.response.PageResponse;

public interface PayrollPeriodService {
    PayrollPeriodResponse create(PayrollPeriodRequest request);
    PayrollPeriodResponse update(String id, PayrollPeriodRequest request);
    PayrollPeriodResponse lock(String id);
    PayrollPeriodResponse unlock(String id);
    PayrollPeriodResponse close(String id);
    PayrollPeriodResponse getById(String id);
    PageResponse<PayrollPeriodResponse> getAll(String orgId, Integer month, Integer year, Integer status,
                                               Integer pageNumber, Integer pageSize);
}
