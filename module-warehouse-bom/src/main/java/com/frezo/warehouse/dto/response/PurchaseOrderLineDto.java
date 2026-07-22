package com.frezo.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderLineDto {
    private String id;
    private String prLineId;
    private String productId;
    private String warehouseId;
    private Double qty;
    private Double receivedQty;
    private String note;
}
