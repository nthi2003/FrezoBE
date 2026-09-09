package com.frezo.qtbv.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AssetTransferCreateRequest {
    private String requestType;
    private String personId;
    private String personName;
    private String reason;
    private LocalDate plannedDate;
}
