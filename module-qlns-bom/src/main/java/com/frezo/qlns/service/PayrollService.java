package com.frezo.qlns.service;

import com.frezo.common.response.PageResponse;
import com.frezo.qlns.dto.request.PayrollFilter;
import com.frezo.qlns.dto.response.PayrollResponse;

public interface PayrollService {

    /** ThiNVQ : Tính lương tháng cho 1 nhân viên theo contractId */
    PayrollResponse calculateMonthlyPayroll(String contractId, Integer month, Integer year);

    /** ThiNVQ : Tính lương hàng loạt cho tất cả nhân viên active trong tháng */
    void calculateAllPayroll(Integer month, Integer year);

    /** ThiNVQ : Cập nhật thưởng/khấu trừ và ghi chú */
    PayrollResponse updateBonus(String id, Double bonus, Double deduction, String note);

    /** ThiNVQ : Xác nhận bảng lương (DRAFT → CONFIRMED) */
    PayrollResponse confirm(String id);

    /** ThiNVQ : Đánh dấu đã thanh toán (CONFIRMED → PAID) */
    PayrollResponse pay(String id);

    /** ThiNVQ : Danh sách bảng lương có lọc và phân trang */
    PageResponse<PayrollResponse> getAll(PayrollFilter filter);

    PayrollResponse getById(String id);
}
