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

    /** P1 — Pay for Position */
    private Double salaryP1;
    /** P2 — Pay for Person */
    private Double salaryP2;
    /** P3 — Pay for Performance (mục tiêu) */
    private Double salaryP3;
    private Double salaryFixed;
    private Double salaryTotalAtFullKpi;

    private Double cogsPct;
    private Double riskPct;
    private Double commissionPct;
    private Double contributionMarginPct;

    /** Doanh thu cần đạt để cover P1+P2+P3 */
    private Double calculatedRevenueTarget;

    private String currency;
    private Boolean fromContract;
    private String note;
}
