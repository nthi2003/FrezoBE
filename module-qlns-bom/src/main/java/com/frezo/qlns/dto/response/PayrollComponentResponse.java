package com.frezo.qlns.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PayrollComponentResponse {
    private String id;
    private String code;
    private String name;
    private String nature;
    private String taxableType;
    private Boolean taxDeductible;
    private String quotaType;
    private BigDecimal quotaValue;
    private BigDecimal defaultValue;
    private Boolean activated;
}
