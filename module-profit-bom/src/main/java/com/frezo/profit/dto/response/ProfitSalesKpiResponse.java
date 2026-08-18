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
public class ProfitSalesKpiResponse {

    private Integer year;
    private Integer month;
    private String currency;
    private List<SalesKpiLine> employees;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SalesKpiLine {
        private String personId;
        private String personName;
        private String contractId;
        private Double revenueTarget;
        private Double actualRevenue;
        private Double shortfall;
        private Double achievementPct;
        private Double monthlySalary;
        private Double commissionPct;
        private Double estimatedCommission;
        private Double salaryP1;
        private Double salaryP2;
        private Double salaryP3;
        private Double salaryP3Earned;
        private Double estimatedTotalComp;
        private Boolean targetAutoCalculated;
    }
}
