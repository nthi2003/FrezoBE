package com.frezo.warehouse.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class GinCreateRequest {

    @NotBlank(message = "Kho không được để trống")
    private String warehouseId;

    private String customerId;
    private String orderId;
    private String issueType;

    /** Số chứng từ / hóa đơn xuất. */
    private String documentNo;

    private LocalDate documentDate;

    /** Kho đích (chuyển kho nội bộ). */
    private String transferWarehouseId;

    private String note;

    private List<GinItemRequest> items;

    @Data
    public static class GinItemRequest {
        @NotBlank(message = "Sản phẩm không được để trống")
        private String productId;

        private String batchId;

        @NotBlank(message = "Số lượng yêu cầu không được để trống")
        private Double qtyRequested;

        private Double unitCost;

        private String locationId;
    }
}
