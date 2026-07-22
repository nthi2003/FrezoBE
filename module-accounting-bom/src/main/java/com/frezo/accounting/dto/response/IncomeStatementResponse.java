package com.frezo.accounting.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeStatementResponse {
    private String from;
    private String to;
    private BigDecimal totalRevenue;
    private BigDecimal totalExpense;
    private BigDecimal netIncome;
    @Builder.Default
    private List<ReportLine> revenues = new ArrayList<>();
    @Builder.Default
    private List<ReportLine> expenses = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportLine {
        private String accountCode;
        private String accountName;
        private BigDecimal amount;
    }
}
