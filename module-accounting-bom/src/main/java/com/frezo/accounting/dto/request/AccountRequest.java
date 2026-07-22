package com.frezo.accounting.dto.request;

import com.frezo.accounting.common.AccountType;
import com.frezo.accounting.common.AccountingStandard;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AccountRequest {
    @NotBlank
    private String code;

    @NotBlank
    private String name;

    @NotNull
    private AccountType type;

    @NotNull
    private AccountingStandard standard;

    @NotNull
    private Integer level;

    private String parentId;
    private Boolean postable;
    private Boolean requiresPartner;
    private BigDecimal openingBalance;
    private Boolean active;
    private String description;
}
