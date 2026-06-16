package com.frezo.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class GrnCreateRequest {
    private String purchaseOrderId;

    @NotBlank(message = "Kho không được để trống")
    private String warehouseId;

    private String supplierId;

    private String note;

    private List<GrnItemRequest> items;

    @Data
    public static class GrnItemRequest {
        @NotBlank(message = "Sản phẩm không được để trống")
        private String productId;

        private String batchId;

        @NotBlank(message = "Số lượng dự kiến không được để trống")
        private Double qtyExpected;

        private Double qtyReceived;

        private Double unitCost;

        private String locationId;
    }
}
