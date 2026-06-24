package com.frezo.warehouse.dto.response;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class StockMovementResponse {
    private String id;
    private String productId;
    private String batchId;
    private String warehouseId;
    private String locationId;
    private String transactionType;
    private Double quantity;
    private Double unitCost;
    private Double totalValue;
    private String referenceType;
    private String referenceId;
    private String note;
    private LocalDateTime createdDate;
    private String createdBy;
    private Double runningBalance;
}
