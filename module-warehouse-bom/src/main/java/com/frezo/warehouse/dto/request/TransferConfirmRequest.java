package com.frezo.warehouse.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class TransferConfirmRequest {
    private List<TransferConfirmItem> items;

    @Data
    public static class TransferConfirmItem {
        private String itemId;
        private Double qtyTransferred;
        private String fromLocationId;
        private String toLocationId;
    }
}
