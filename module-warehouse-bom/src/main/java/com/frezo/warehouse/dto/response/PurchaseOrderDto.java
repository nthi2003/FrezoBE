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
    /** Legacy field — prefer purchaseRequestId on FE. */
    private String prId;
    private String purchaseRequestId;
    private String supplierId;
    private String supplierName;
    private String warehouseId;
    private String warehouseName;
    private String status;
    private String note;
    private String confirmedAt;
    private String receivedAt;
    /** Legacy field — prefer createdAt on FE. */
    private String createdDate;
    private String createdAt;
    private List<PurchaseOrderLineDto> lines;
    /** GRN gắn PO — hiển thị hóa đơn NCC trên màn PO. */
    private List<LinkedGrnSummary> linkedGrns;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LinkedGrnSummary {
        private String id;
        private String grnCode;
        private String status;
        private String invoiceNo;
        private String invoiceDate;
    }
}
