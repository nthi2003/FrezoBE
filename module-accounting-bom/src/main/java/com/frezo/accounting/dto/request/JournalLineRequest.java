package com.frezo.accounting.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class JournalLineRequest {

    @NotBlank
    private String accountCode;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal debit;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal credit;

    private String description;

    private String departmentId;
    private String partnerType;
    private String partnerId;
    private String partnerName;
    private String projectId;
}
