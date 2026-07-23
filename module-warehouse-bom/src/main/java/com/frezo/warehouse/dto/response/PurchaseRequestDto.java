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
public class PurchaseRequestDto {
    private String id;
    private String code;
    private String supplierId;
    private String warehouseId;
    private String status;
    private String note;
    private String approvalRequestId;
    /** LNK-05: true khi submit bypass vì warehouse.pr.approval.required=false. */
    private Boolean approvalBypassed;
    private String submittedAt;
    private String createdDate;
    private List<PurchaseRequestLineDto> lines;
}
