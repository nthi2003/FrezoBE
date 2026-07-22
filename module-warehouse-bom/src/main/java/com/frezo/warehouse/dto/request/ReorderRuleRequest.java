package com.frezo.warehouse.dto.request;

import lombok.Data;

@Data
public class ReorderRuleRequest {
    private String warehouseId;
    private String productId;
    private Double minQty;
    private Double maxQty;
    private Double reorderQty;
    private Boolean active;
}
