package com.frezo.warehouse.dto.request;

import lombok.Data;

@Data
public class StockTakeLineRequest {
    private String id;
    private String productId;
    private Double countedQty;
    private String note;
}
