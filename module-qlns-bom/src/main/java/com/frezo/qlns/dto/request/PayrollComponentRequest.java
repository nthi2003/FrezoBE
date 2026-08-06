package com.frezo.qlns.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayrollComponentRequest {
    @NotBlank
    private String code;
    @NotBlank
    private String name;
    /** INCOME | DEDUCTION */
    @NotBlank
    private String nature;
    /** FULL | PARTIAL | NONE */
    private String taxableType;
    private Boolean taxDeductible;
    /** FIXED | PERCENT */
    private String quotaType;
    private BigDecimal quotaValue;
    private BigDecimal defaultValue;
    private Boolean activated = true;
}
