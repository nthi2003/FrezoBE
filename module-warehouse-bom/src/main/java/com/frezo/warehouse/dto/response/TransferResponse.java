package com.frezo.warehouse.dto.response;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TransferResponse {
    private String id;
    private String transferCode;
    private String fromWarehouseId;
    private String toWarehouseId;
    private String status;
    private Double totalValue;
    private String transferredBy;
    private LocalDateTime transferredAt;
    private String note;
    private LocalDateTime createdDate;
    private List<TransferItemResponse> items;

    @Data
    public static class TransferItemResponse {
        private String id;
        private String transferId;
        private String productId;
        private String batchId;
        private Double qtyTransferred;
        private Double unitCost;
        private String fromLocationId;
        private String toLocationId;
    }
}
