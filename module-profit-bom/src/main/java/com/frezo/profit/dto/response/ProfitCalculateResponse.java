package com.frezo.profit.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitCalculateResponse {
    private String productId;
    private String productName;
    private String productCode;
    private Double quantity;
    private Double salePrice;
    private Double unitCost;
    private String costSource;
    private String settingsScope;

    private Double revenue;
    private Double cogs;
    private Double grossProfit;
    private Double riskCost;
    private Double laborCost;
    private Double fixedCostAllocated;
    private Double netProfit;
    private Double netProfitMarginPct;
    private Double targetProfitMarginPct;
}
