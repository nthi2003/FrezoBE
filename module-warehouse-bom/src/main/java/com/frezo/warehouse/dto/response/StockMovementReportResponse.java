package com.frezo.warehouse.dto.response;

import lombok.Data;
import java.util.List;

@Data
public class StockMovementReportResponse {
    private String productId;
    private List<StockMovementResponse> movements;
    private Double openingBalance;
    private Double totalIn;
    private Double totalOut;
    private Double closingBalance;
}
