package com.frezo.qlns.service;

import com.frezo.qlns.dto.request.PayrollPeriodRequest;
import com.frezo.qlns.dto.response.PayrollPeriodResponse;
import com.frezo.common.response.PageResponse;

public interface PayrollPeriodService {
    PayrollPeriodResponse create(PayrollPeriodRequest request);
    PayrollPeriodResponse update(String id, PayrollPeriodRequest request);

    /** Khoá kỳ lương + START workflow duyệt (theo definition PAYROLL_DEFAULT). */
    PayrollPeriodResponse lock(String id);

    /** Mở khoá — chỉ cho phép khi workflow chưa chạy hoặc đã bị cancel. */
    PayrollPeriodResponse unlock(String id);

    /** Đóng thủ công (dùng khi không cần workflow hoặc override). */
    PayrollPeriodResponse close(String id);

    /**
     * Duyệt bước hiện tại của workflow instance. Nếu là bước cuối → kỳ lương chuyển
     * sang status=2 (Đã đóng, sẵn sàng chi lương).
     */
    PayrollPeriodResponse approve(String id, String note);

    /** Từ chối bước hiện tại → workflow terminate, kỳ lương quay về status=0 (Mở). */
    PayrollPeriodResponse reject(String id, String reason);

    PayrollPeriodResponse getById(String id);
    PageResponse<PayrollPeriodResponse> getAll(String orgId, Integer month, Integer year, Integer status,
                                               Integer pageNumber, Integer pageSize);
}
