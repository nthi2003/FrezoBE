package com.frezo.warehouse.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GinResponse {
    private String id;
    private String ginCode;
    private String warehouseId;
    private String customerId;
    private String orderId;
    private String issueType;
    private String status;
    private Double totalValue;
    private String issuedBy;
    private LocalDateTime issuedAt;
    private String note;
    private LocalDateTime createdDate;
    private List<GinItemResponse> items;

    @Data
    public static class GinItemResponse {
        private String id;
        private String ginId;
        private String productId;
        private String batchId;
        private Double qtyRequested;
        private Double qtyIssued;
        private Double unitCost;
        private String locationId;
    }
}
