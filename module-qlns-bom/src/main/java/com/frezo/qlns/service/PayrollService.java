package com.frezo.qlns.service;

import com.frezo.common.response.PageResponse;
import com.frezo.qlns.dto.request.PayrollFilter;
import com.frezo.qlns.dto.response.PayrollDetailResponse;
import com.frezo.qlns.dto.response.PayrollResponse;

import java.util.List;

public interface PayrollService {

    PayrollResponse calculateMonthlyPayroll(String personId, Integer month, Integer year);

    void calculateAllPayroll(Integer month, Integer year);

    PayrollResponse updateBonus(String id, Double bonus, Double deduction, String note);

    PayrollResponse confirm(String id);

    PayrollResponse pay(String id);

    PageResponse<PayrollResponse> getAll(PayrollFilter filter);

    PayrollResponse getById(String id);

    List<PayrollDetailResponse> getPayrollDetails(String payrollId);

    void deletePayroll(String id);
}
