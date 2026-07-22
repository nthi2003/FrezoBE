package com.frezo.accounting.common;

/**
 * Loại tài khoản kế toán theo bản chất kinh tế.
 * <p>Áp dụng cho cả Thông tư 133/2016 (SME) và Thông tư 99/2025 (thay thế TT 200).
 */
public enum AccountType {
    /** Tài sản — Debit tăng. Số hiệu bắt đầu bằng 1 hoặc 2. */
    ASSET,
    /** Nợ phải trả — Credit tăng. Số hiệu bắt đầu bằng 3. */
    LIABILITY,
    /** Vốn chủ sở hữu — Credit tăng. Số hiệu bắt đầu bằng 4. */
    EQUITY,
    /** Doanh thu — Credit tăng. Số hiệu bắt đầu bằng 5, 7. */
    REVENUE,
    /** Chi phí — Debit tăng. Số hiệu bắt đầu bằng 6, 8. */
    EXPENSE,
    /** Tài khoản trung gian (xác định KQKD 911). */
    CLEARING
}
