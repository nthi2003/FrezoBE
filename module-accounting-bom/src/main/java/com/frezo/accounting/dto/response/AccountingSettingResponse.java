package com.frezo.accounting.dto.response;

import com.frezo.accounting.common.AccountingStandard;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountingSettingResponse {
    private String id;
    private AccountingStandard standard;
    private String baseCurrency;
    private String payrollPostingStrategy;
    private String accSalaryExpense;
    private String accSalaryPayable;
    private String accBhxhPayable;
    private String accBhytPayable;
    private String accBhtnPayable;
    private String accPitPayable;
    private String accUnionFee;
}
