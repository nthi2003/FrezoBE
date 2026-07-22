package com.frezo.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankStatementLineDto {
    private String id;
    private String statementId;
    private String txnDate;
    private String description;
    private String refCode;
    private Double debit;
    private Double credit;
    private Double balance;
    private String matchStatus;
    private String matchedJournalLineId;
}
