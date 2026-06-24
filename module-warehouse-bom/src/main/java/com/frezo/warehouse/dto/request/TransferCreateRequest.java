package com.frezo.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.List;

@Data
public class TransferCreateRequest {

    @NotBlank(message = "Kho nguồn không được để trống")
    private String fromWarehouseId;

    @NotBlank(message = "Kho đích không được để trống")
    private String toWarehouseId;

    private String note;

    private List<TransferItemRequest> items;

    @Data
    public static class TransferItemRequest {
        @NotBlank(message = "Sản phẩm không được để trống")
        private String productId;

        private String batchId;

        @NotBlank(message = "Số lượng chuyển không được để trống")
        private Double qtyTransferred;

        private Double unitCost;

        private String fromLocationId;
        private String toLocationId;
    }
}
