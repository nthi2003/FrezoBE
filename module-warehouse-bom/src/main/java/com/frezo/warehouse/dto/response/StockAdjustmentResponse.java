package com.frezo.warehouse.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class StockAdjustmentResponse {
    private String id;
    private String adjustmentCode;
    private String warehouseId;
    private String status;
    private Double totalDiffValue;
    private String adjustedBy;
    private LocalDateTime adjustedAt;
    private String note;
    private LocalDateTime createdDate;
    private List<AdjustmentItemResponse> items;

    @Data
    public static class AdjustmentItemResponse {
        private String id;
        private String adjustmentId;
        private String productId;
        private String batchId;
        private String locationId;
        private Double expectedQty;
        private Double actualQty;
        private Double diffQty;
        private Double unitCost;
        private Double totalDiffValue;
        private String reason;
    }
}
