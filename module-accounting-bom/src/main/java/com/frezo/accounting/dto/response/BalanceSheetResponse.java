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
public class BalanceSheetResponse {
    private String from;
    private String to;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal totalEquity;
    @Builder.Default
    private List<ReportLine> assets = new ArrayList<>();
    @Builder.Default
    private List<ReportLine> liabilities = new ArrayList<>();
    @Builder.Default
    private List<ReportLine> equity = new ArrayList<>();

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
