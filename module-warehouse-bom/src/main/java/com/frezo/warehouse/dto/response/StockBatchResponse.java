package com.frezo.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockBatchResponse {
    private String id;
    private String batchCode;
    private String productId;
    private String productCode;
    private String productName;
    private String warehouseId;
    private String warehouseName;
    private String supplierId;
    private String supplierName;
    private String grnId;
    private String grnItemId;
    private String warehouseLocationId;
    private String locationLabel;
    private String receivedDate;
    private String expiryDate;
    private Double qtyOnHand;
    private String status;
    private Integer daysToExpiry;
    private String expiryWarning;
}
