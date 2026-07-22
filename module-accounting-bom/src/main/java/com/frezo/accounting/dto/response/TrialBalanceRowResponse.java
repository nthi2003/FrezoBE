package com.frezo.accounting.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Một dòng trên Bảng cân đối phát sinh.
 */
@Data
@Builder
public class TrialBalanceRowResponse {
    private String accountId;
    private String accountCode;
    private String accountName;

    /** Số dư đầu kỳ Nợ. */
    private BigDecimal openingDebit;
    /** Số dư đầu kỳ Có. */
    private BigDecimal openingCredit;

    /** Phát sinh Nợ trong kỳ. */
    private BigDecimal periodDebit;
    /** Phát sinh Có trong kỳ. */
    private BigDecimal periodCredit;

    /** Số dư cuối kỳ Nợ. */
    private BigDecimal closingDebit;
    /** Số dư cuối kỳ Có. */
    private BigDecimal closingCredit;
}
