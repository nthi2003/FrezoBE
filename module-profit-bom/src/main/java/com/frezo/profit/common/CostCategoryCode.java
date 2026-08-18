package com.frezo.profit.common;

/**
 * Cost center codes — extensible per market (VN default set; EU markets add via DB seed).
 */
public enum CostCategoryCode {
    SALES_VARIABLE,
    FIXED_IT,
    FIXED_LOGISTICS,
    FIXED_QC,
    FIXED_ADMIN,
    VARIABLE_RISK,
    VARIABLE_COMMISSION
}
