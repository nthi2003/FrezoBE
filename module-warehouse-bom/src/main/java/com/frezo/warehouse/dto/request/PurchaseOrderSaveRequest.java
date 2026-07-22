package com.frezo.warehouse.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class PurchaseOrderSaveRequest {
    private String supplierId;
    private String warehouseId;
    private String note;
    private String prId;
    private List<Line> lines;

    @Data
    public static class Line {
        private String productId;
        private String warehouseId;
        private Double qty;
        private String prLineId;
        private String note;
    }
}
