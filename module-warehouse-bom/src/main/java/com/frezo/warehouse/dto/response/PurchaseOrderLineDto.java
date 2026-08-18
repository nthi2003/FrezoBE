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
    private String productCode;
    private String productName;
    private String warehouseId;
    /** Legacy field — prefer qtyOrdered on FE. */
    private Double qty;
    private Double qtyOrdered;
    /** Legacy field — prefer qtyReceived on FE. */
    private Double receivedQty;
    private Double qtyReceived;
    private Double unitPrice;
    private String note;
}
