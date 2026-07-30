package com.frezo.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAlertDto {
    private String id;
    private String warehouseId;
    private String warehouseName;
    private String productId;
    private String productCode;
    private String productName;
    private String categoryName;
    private Double currentQty;
    private Double minQty;
    private String severity;
    private String status;
    private String triggeredAt;
    private String dismissedAt;
    /** LOW_STOCK / EXPIRY_SOON */
    private String alertType;
    private String batchId;
    private String batchCode;
    private String expiryDate;
    private Integer daysToExpiry;
}
