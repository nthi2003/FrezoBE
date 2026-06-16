package com.frezo.warehouse.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GrnResponse {
    private String id;
    private String grnCode;
    private String purchaseOrderId;
    private String warehouseId;
    private String supplierId;
    private String status;
    private Double totalValue;
    private String receivedBy;
    private LocalDateTime receivedAt;
    private String note;
    private LocalDateTime createdDate;
    private List<GrnItemResponse> items;

    @Data
    public static class GrnItemResponse {
        private String id;
        private String grnId;
        private String productId;
        private String batchId;
        private Double qtyExpected;
        private Double qtyReceived;
        private Double unitCost;
        private String locationId;
    }
}
