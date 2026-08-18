package com.frezo.profit.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProfitSettingRequest {

    @NotBlank
    private String scope;

    private String scopeRefId;

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
}
