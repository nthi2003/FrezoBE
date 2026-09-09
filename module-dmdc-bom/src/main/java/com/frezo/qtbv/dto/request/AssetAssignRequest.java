package com.frezo.qtbv.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AssetAssignRequest {
    private String personId;
    private String personName;
    private LocalDate actionDate;
    private String note;
}
