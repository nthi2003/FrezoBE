package com.frezo.profit.dto.request;

import com.frezo.profit.common.CostCategoryCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProfitEmployeeAllocationRequest {

    @NotBlank
    private String personId;

    private String contractId;

    @NotNull
    private CostCategoryCode costCategoryCode;

    private Double monthlySalary;

    private Boolean usePayrollSource;

    private Double revenueTargetMonthly;

    private Double commissionPct;

    private Boolean autoCalculateTarget;

    private String currency;

    private Boolean active;

    private String note;
}
