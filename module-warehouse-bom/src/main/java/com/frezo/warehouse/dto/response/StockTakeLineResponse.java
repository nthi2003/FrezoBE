package com.frezo.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockTakeLineResponse {
    private String id;
    private String productId;
    private Double systemQty;
    private Double countedQty;
    private Double varianceQty;
    private String note;
}
