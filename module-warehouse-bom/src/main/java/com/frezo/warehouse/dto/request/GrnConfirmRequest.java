package com.frezo.warehouse.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class GrnConfirmRequest {
    private List<GrnConfirmItem> items;

    @Data
    public static class GrnConfirmItem {
        private String itemId;
        private Double qtyReceived;
        private String batchCode;
        private String locationId;
    }
}
