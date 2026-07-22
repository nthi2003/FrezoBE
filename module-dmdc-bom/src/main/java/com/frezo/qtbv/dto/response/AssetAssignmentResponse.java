package com.frezo.qtbv.dto.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class AssetAssignmentResponse {
    private String id;
    private String assetId;
    private String action;
    private String personId;
    private String personName;
    private LocalDate actionDate;
    private String note;
    private BigDecimal cost;
    private String createdBy;
    private String createdDate;
}
