package com.frezo.accounting.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Sổ cái theo tài khoản trong 1 khoảng thời gian.
 */
@Data
@Builder
public class GLResponse {
    private String accountId;
    private String accountCode;
    private String accountName;
    private LocalDate from;
    private LocalDate to;

    private BigDecimal openingDebit;
    private BigDecimal openingCredit;

    private BigDecimal periodDebit;
    private BigDecimal periodCredit;

    private BigDecimal closingDebit;
    private BigDecimal closingCredit;

    private List<GLLineResponse> lines;
}
