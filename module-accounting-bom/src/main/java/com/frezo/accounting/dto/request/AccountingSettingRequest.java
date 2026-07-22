package com.frezo.accounting.dto.request;

import com.frezo.accounting.common.AccountingStandard;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AccountingSettingRequest {

    @NotNull
    private AccountingStandard standard;

    /** ISO currency code (VND, USD...). Mặc định VND. */
    private String baseCurrency;

    /** AGGREGATE_PERIOD | PER_DEPT | PER_EMPLOYEE. */
    private String payrollPostingStrategy;

    private String accSalaryExpense;
    private String accSalaryPayable;
    private String accBhxhPayable;
    private String accBhytPayable;
    private String accBhtnPayable;
    private String accPitPayable;
    private String accUnionFee;

    /** Nếu true và chưa có COA, seed chart-of-accounts theo standard. */
    private Boolean seedCoa;
}
