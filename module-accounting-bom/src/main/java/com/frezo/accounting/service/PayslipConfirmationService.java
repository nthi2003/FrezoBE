package com.frezo.accounting.service;

import com.frezo.accounting.entity.PayslipConfirmation;

import java.util.Optional;

public interface PayslipConfirmationService {

    Optional<PayslipConfirmation> find(String payrollId);

    /**
     * Nhân viên confirm đã nhận payslip. Idempotent: nếu đã confirm, cập nhật ghi chú/timestamp mới.
     */
    PayslipConfirmation confirm(String payrollId, String personId, String note,
                                String ip, String device);
}
