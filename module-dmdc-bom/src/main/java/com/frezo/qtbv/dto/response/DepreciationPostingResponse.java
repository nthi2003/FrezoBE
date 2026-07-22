package com.frezo.qtbv.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepreciationPostingResponse {

    private String id;
    private Integer periodYear;
    private Integer periodMonth;
    private BigDecimal totalAmount;
    private Integer scheduleCount;
    private String journalEntryId;
    private String status;
    private String errorMessage;
}
