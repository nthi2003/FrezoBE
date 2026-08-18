package com.frezo.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductStockLevelDto {
    private String warehouseId;
    private String warehouseCode;
    private String warehouseName;
    private Double quantityOnHand;
    private Double quantityReserved;
    private Double quantityAvailable;
}
