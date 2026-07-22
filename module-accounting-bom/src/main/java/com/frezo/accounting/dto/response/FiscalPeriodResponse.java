package com.frezo.accounting.dto.response;

import com.frezo.accounting.common.PeriodStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class FiscalPeriodResponse {
    private String id;
    private String fiscalYearId;
    private Integer month;
    private Integer year;
    private LocalDate startDate;
    private LocalDate endDate;
    private PeriodStatus status;
    private LocalDateTime closedAt;
    private String closedBy;
}
