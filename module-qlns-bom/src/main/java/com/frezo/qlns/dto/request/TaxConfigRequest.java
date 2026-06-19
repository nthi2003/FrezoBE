package com.frezo.qlns.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TaxConfigRequest {
    private Integer year;
    private Integer bracketOrder;
    private BigDecimal fromAmount;
    private BigDecimal toAmount;
    private BigDecimal rate;
    private BigDecimal deductAmount;
    private BigDecimal personalDeduction;
    private BigDecimal dependentDeduction;
    private Boolean isActive;
}
