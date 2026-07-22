package com.frezo.accounting.common;

/**
 * Trạng thái kỳ kế toán (tháng).
 */
public enum PeriodStatus {
    /** Đang mở — cho phép posting. */
    OPEN,
    /** Đã khóa — không cho posting mới. Chỉ kế toán trưởng có thể mở lại. */
    CLOSED,
    /** Khoá vĩnh viễn (đã quyết toán năm). */
    LOCKED
}
