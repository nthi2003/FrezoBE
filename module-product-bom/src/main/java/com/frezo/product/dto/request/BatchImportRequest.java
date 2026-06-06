package com.frezo.product.dto.request;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class BatchImportRequest {
    private String supplierId;
    private List<BatchItem> items;

    @Data
    public static class BatchItem {
        private String productCode;
        private String growingArea;
        private Double quantity;
        private Double costPrice;
        private LocalDate expiryDate;
    }
}
