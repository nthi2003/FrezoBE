package com.frezo.accounting.common;

/**
 * Trạng thái chứng từ ghi sổ.
 */
public enum JournalStatus {
    /** Nháp — có thể sửa/xoá tự do. Chưa vào GL. */
    DRAFT,
    /** Đã ghi sổ (posted) — vào GL, ảnh hưởng số dư. Chỉ có thể reverse (đảo). */
    POSTED,
    /** Đã đảo (reversal entry). Bút toán gốc bị vô hiệu hóa cân đối. */
    REVERSED
}
