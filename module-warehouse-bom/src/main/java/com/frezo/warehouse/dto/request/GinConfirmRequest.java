package com.frezo.warehouse.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class GinConfirmRequest {
    private List<GinConfirmItem> items;

    @Data
    public static class GinConfirmItem {
        private String itemId;
        private Double qtyIssued;
        private String batchCode;
        private String locationId;
    }
}
