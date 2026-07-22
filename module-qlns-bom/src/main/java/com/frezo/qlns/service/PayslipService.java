package com.frezo.qlns.service;

import com.frezo.qlns.dto.response.PayrollYtdResponse;
import com.frezo.qlns.dto.response.PayslipFormulaResponse;
import com.frezo.qlns.dto.response.PayslipResponse;

/**
 * Mobile-focused payslip service. Cung cấp full DTO cho màn hình Payslip Detail
 * và các endpoint YTD / Formulas / Confirmation.
 */
public interface PayslipService {

    /** Full payslip DTO breakdown chi tiết cho mobile. */
    PayslipResponse getPayslip(String payrollId);

    /** YTD summary + list 12 tháng. */
    PayrollYtdResponse getYtd(String personId, Integer year);

    /** Static formulas (không phụ thuộc payroll cụ thể). */
    PayslipFormulaResponse getFormulas();

    /** Nhân viên confirm đã nhận payslip. */
    PayslipResponse confirmReceived(String payrollId, String note, String ip, String device);
}
