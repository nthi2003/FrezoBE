package com.frezo.warehouse.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class StockAdjustmentCreateRequest {
    private String warehouseId;
    private String note;
    private List<AdjustmentItemRequest> items;

    @Data
    public static class AdjustmentItemRequest {
        private String productId;
        private String batchId;
        private String locationId;
        private Double expectedQty;
        private Double actualQty;
        private Double unitCost;
        private String reason;
    }
}
