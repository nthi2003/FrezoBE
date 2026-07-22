package com.frezo.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequestLineDto {
    private String id;
    private String productId;
    private String warehouseId;
    private Double qty;
    private String stockAlertId;
    private String note;
}
