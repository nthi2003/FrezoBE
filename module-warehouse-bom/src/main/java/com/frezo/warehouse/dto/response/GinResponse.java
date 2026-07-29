package com.frezo.warehouse.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GinResponse {
    private String id;
    private String ginCode;
    private String warehouseId;
    private String warehouseName;
    private String warehouseCode;
    private String customerId;
    private String customerName;
    private String orderId;
    private String issueType;
    private String status;
    private String documentNo;
    private LocalDate documentDate;
    private String transferWarehouseId;
    private String transferWarehouseName;
    private String approvedBy;
    private LocalDateTime approvedAt;
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
