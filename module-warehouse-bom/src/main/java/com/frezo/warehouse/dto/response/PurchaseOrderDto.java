package com.frezo.warehouse.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderDto {
    private String id;
    private String code;
    private String prId;
    private String supplierId;
    private String warehouseId;
    private String status;
    private String note;
    private String confirmedAt;
    private String receivedAt;
    private String createdDate;
    private List<PurchaseOrderLineDto> lines;
}
