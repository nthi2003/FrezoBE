package com.frezo.profit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitDashboardResponse {

    private Integer year;
    private Integer month;
    private String currency;
    private Double actualRevenue;
    private Double breakEvenRevenue;
    private Double targetRevenue;
    private Double revenueShortfall;
    private Double targetShortfall;
    private Double totalFixedCosts;
    private Double totalVariableCosts;
    private Double estimatedCogs;
    private Double netProfit;
    private Double corporateTaxAmount;
    private Double netProfitAfterTax;
    private Double corporateTaxPct;
    private Double monthlyBurn;
    private Double contributionMarginPct;

    private Integer employeeCount;
    private Integer salesStaffCount;

    private List<CostBreakdownLine> costBreakdown;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CostBreakdownLine {
        private String code;
        private String label;
        private String costType;
        private Double amount;
    }
}
