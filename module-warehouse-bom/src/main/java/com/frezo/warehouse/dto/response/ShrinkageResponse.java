package com.frezo.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShrinkageResponse {
    private String id;
    private String shrinkageCode;
    private String warehouseId;
    private String warehouseName;
    private String status;
    private String note;
    private String confirmedAt;
    private String createdAt;
    private List<ShrinkageLineResponse> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShrinkageLineResponse {
        private String id;
        private String batchId;
        private String batchCode;
        private String productId;
        private String productCode;
        private String productName;
        private String reason;
        private Double qty;
        private String note;
        private String expiryDate;
    }
}
