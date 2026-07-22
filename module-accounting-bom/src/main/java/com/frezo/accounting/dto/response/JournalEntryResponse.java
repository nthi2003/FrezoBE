package com.frezo.accounting.dto.response;

import com.frezo.accounting.common.JournalStatus;
import com.frezo.accounting.common.PostingSource;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class JournalEntryResponse {
    private String id;
    private String code;
    private LocalDate postingDate;
    private LocalDate documentDate;
    private String periodId;
    private String description;
    private PostingSource sourceType;
    private String sourceId;
    private String idempotencyKey;
    private JournalStatus status;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    private LocalDateTime postedAt;
    private String postedBy;
    private String reversalOfId;
    private List<JournalLineResponse> lines;
}
