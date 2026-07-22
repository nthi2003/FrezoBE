package com.frezo.qlns.service;

/**
 * Hạch toán bảng lương của kỳ (month/year) sang Accounting GL.
 * Sinh 1 bút toán tổng hợp cho toàn bộ payroll của kỳ (aggregate strategy).
 * <p>Idempotent qua idempotency key = "payroll:{year}-{month}".
 */
public interface PayrollGLPostingService {

    /**
     * Post payroll period sang GL. Nếu đã post rồi (idempotency), trả về entry cũ.
     *
     * @return journalEntryId đã sinh (hoặc entry cũ)
     */
    String postPeriod(Integer month, Integer year);

    /**
     * Rollback (đảo) chứng từ payroll đã post cho kỳ này.
     * Tạo bút toán REVERSAL, cho phép chỉnh sửa & post lại.
     */
    String reversePeriod(Integer month, Integer year, String reason);
}
