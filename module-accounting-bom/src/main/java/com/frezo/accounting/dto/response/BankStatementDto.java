package com.frezo.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankStatementDto {
    private String id;
    private String accountId;
    private String accountCode;
    private String accountName;
    private String importedAt;
    private Integer importedLines;
    private Integer matchedCount;
    private Integer unmatchedCount;
    private String fileName;
    /** OPEN / LOCKED */
    private String status;
}
