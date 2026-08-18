package com.frezo.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockChartPoint {
    private String date;
    private Double quantityOnHand;
    private String transactionType;
    private Double deltaQty;
    private String referenceType;
    private String note;
}
