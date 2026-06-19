package com.frezo.qlns.dto.response;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayrollDetailResponse {
    private String id;
    private String payrollId;
    private String componentCode;
    private String componentName;
    private String componentType;
    private BigDecimal amount;
    private String reference;
    private String description;
    private Integer sortOrder;
}
