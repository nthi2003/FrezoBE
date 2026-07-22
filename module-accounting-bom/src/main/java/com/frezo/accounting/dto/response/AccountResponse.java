package com.frezo.accounting.dto.response;

import com.frezo.accounting.common.AccountType;
import com.frezo.accounting.common.AccountingStandard;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AccountResponse {
    private String id;
    private String code;
    private String name;
    private AccountType type;
    private AccountingStandard standard;
    private Integer level;
    private String parentId;
    private Boolean postable;
    private Boolean requiresPartner;
    private BigDecimal openingBalance;
    private Boolean active;
    private String description;
}
