package com.frezo.profit.dto.request;

import lombok.Data;

@Data
public class ProfitSalesTargetPreviewRequest {

    private String personId;
    private String contractId;
    private Double commissionPct;
}
