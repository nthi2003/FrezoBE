package com.frezo.accounting.common;

/**
 * Chuẩn kế toán Việt Nam áp dụng cho company.
 * <ul>
 *   <li>{@link #TT133} — Thông tư 133/2016/TT-BTC — Doanh nghiệp nhỏ và vừa (SME).</li>
 *   <li>{@link #TT99} — Thông tư 99/2025/TT-BTC — thay thế TT 200/2014 kể từ 01/01/2026,
 *       áp dụng cho DN mọi lĩnh vực, mọi thành phần kinh tế.</li>
 * </ul>
 */
public enum AccountingStandard {
    TT133,
    TT99
}
