package com.frezo.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReorderRuleDto {
    private String id;
    private String warehouseId;
    private String warehouseName;
    private String productId;
    private String productCode;
    private String productName;
    private String categoryName;
    private Double minQty;
    private Double maxQty;
    private Double reorderQty;
    private Boolean active;
    private String updatedAt;
}
