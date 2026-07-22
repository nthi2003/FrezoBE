package com.frezo.accounting.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class JournalLineResponse {
    private String id;
    private String journalEntryId;
    private Integer lineNo;
    private String accountId;
    private String accountCode;
    private String accountName;
    private BigDecimal debit;
    private BigDecimal credit;
    private String description;
    private String departmentId;
    private String partnerType;
    private String partnerId;
    private String partnerName;
    private String projectId;
}
