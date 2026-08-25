package com.frezo.profit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitSalesTargetPreviewDto {

    private String personId;
    private String contractId;
    private String contractCode;
    private Double salaryP1;
    private Double salaryP2;
    private Double salaryP3;
    private Double salaryFixed;
    private Double salaryTotalAtFullKpi;
    private Double cogsPct;
    private Double riskPct;
    private Double commissionPct;
    private Double contributionMarginPct;
    private Double calculatedRevenueTarget;
    private String currency;
    private Boolean fromContract;
    private String note;
}
