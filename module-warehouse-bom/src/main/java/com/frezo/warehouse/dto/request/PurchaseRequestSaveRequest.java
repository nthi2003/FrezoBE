package com.frezo.warehouse.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class PurchaseRequestSaveRequest {
    private String supplierId;
    private String warehouseId;
    private String note;
    private List<Line> lines;

    @Data
    public static class Line {
        private String productId;
        private String warehouseId;
        private Double qty;
        private String stockAlertId;
        private String note;
    }
}
