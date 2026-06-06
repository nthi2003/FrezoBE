package com.frezo.cms.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerExportResponse {
    private String code;
    private String name;
    private String phoneDecrypted;
    private String address;
    private Double creditLimit;
    private Integer debtDaysAllowed;
    private String statusLabel;
}
