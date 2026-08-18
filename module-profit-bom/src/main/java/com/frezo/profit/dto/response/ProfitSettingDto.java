package com.frezo.profit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitSettingDto {
    private String id;
    private String scope;
    private String scopeRefId;
    private String scopeLabel;
    private String name;
    private Double profitMarginPct;
    private Double riskCostPct;
    private Double laborCostPerUnit;
    private Double fixedMonthlyOverhead;
    private Double monthlySalesTargetUnits;
    private Double defaultCogsPct;
    private Double salesCommissionPct;
    private Double corporateTaxPct;
    private String currency;
    private Boolean active;
    private String note;
    private String updatedAt;
}
