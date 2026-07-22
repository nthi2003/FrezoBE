package com.frezo.accounting.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Một dòng trên sổ cái (General Ledger).
 * Mỗi dòng là 1 phát sinh + số dư luỹ kế đến thời điểm đó.
 */
@Data
@Builder
public class GLLineResponse {
    private String journalEntryId;
    private String journalCode;
    private LocalDate postingDate;
    private String description;

    private BigDecimal debit;
    private BigDecimal credit;

    /** Số dư luỹ kế Nợ (nếu TK tài sản/chi phí). */
    private BigDecimal runningDebit;
    /** Số dư luỹ kế Có (nếu TK nguồn vốn/doanh thu). */
    private BigDecimal runningCredit;

    private String partnerName;
    private String departmentId;
}
