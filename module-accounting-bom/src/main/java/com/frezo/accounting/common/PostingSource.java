package com.frezo.accounting.common;

/**
 * Nguồn gốc chứng từ ghi sổ — giúp trace audit ngược lại subledger gốc.
 */
public enum PostingSource {
    /** Nhập tay từ FE (Accountant). */
    MANUAL,
    /** Post từ Payroll (bảng lương tháng). */
    PAYROLL,
    /** Post từ Invoice (bán hàng - CRM/Sales). */
    SALES_INVOICE,
    /** Post từ Purchase Order / GRN (mua hàng - Warehouse). */
    PURCHASE,
    /** Post từ Fixed Asset (khấu hao TSCĐ). */
    DEPRECIATION,
    /** Bút toán đảo (reversal) do người dùng huỷ posting cũ. */
    REVERSAL,
    /** Post từ Cash & Bank (phiếu thu/chi, giao dịch NH). */
    CASH_BANK,
    /** Post từ Inventory (giá vốn 632). */
    INVENTORY
}
