package com.frezo.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchSuggestionDto {
    private String journalEntryLineId;
    private String journalEntryCode;
    private String txnDate;
    private String description;
    private Double amount;
    private Integer score;
    private String reason;
}
