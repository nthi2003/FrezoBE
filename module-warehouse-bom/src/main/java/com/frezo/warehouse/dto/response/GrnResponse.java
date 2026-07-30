package com.frezo.warehouse.dto.response;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class GrnResponse {
    private String id;
    private String grnCode;
    private String purchaseOrderId;
    private String purchaseOrderCode;
    private String warehouseId;
    private String warehouseName;
    private String warehouseCode;
    private String supplierId;
    private String supplierName;
    private String status;
    private String invoiceNo;
    private LocalDate invoiceDate;
    private String approvedBy;
    private LocalDateTime approvedAt;
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
        private String productCode;
        private String productName;
        private String batchId;
        private Double qtyExpected;
        private Double qtyReceived;
        private Double unitCost;
        private String locationId;
    }
}
