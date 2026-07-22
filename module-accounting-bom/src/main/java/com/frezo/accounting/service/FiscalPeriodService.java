package com.frezo.accounting.service;

import com.frezo.accounting.dto.response.FiscalPeriodResponse;
import com.frezo.accounting.entity.FiscalPeriod;

import java.time.LocalDate;
import java.util.List;

/**
 * Quản lý năm tài chính + kỳ kế toán (tháng).
 * Tự động sinh 12 kỳ khi tạo năm mới.
 */
public interface FiscalPeriodService {

    /**
     * Đảm bảo năm tài chính + 12 kỳ tồn tại. Idempotent.
     * Nếu chưa có: tạo FiscalYear + 12 FiscalPeriod (OPEN).
     */
    FiscalPeriodResponse ensureYear(int year);

    /**
     * Tìm kỳ theo ngày (postingDate). Nếu chưa có, tạo cả year + 12 kỳ.
     */
    FiscalPeriod findOrCreateByDate(LocalDate date);

    /** Lấy kỳ theo id; không tồn tại → {@code PERIOD_NOT_FOUND}. */
    FiscalPeriod getRequired(String periodId);

    List<FiscalPeriodResponse> listByYear(int year);

    FiscalPeriodResponse closePeriod(String periodId);

    FiscalPeriodResponse reopenPeriod(String periodId);
}
