package com.frezo.profit.dto.response;

import com.frezo.profit.common.CostCategoryCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfitEmployeeAllocationDto {
    private String id;
    private String personId;
    private String personName;
    private String contractId;
    private CostCategoryCode costCategoryCode;
    private String costCategoryLabel;
    private Double monthlySalary;
    private Boolean usePayrollSource;
    private Double resolvedSalary;
    private Double revenueTargetMonthly;
    private Double commissionPct;
    private Boolean autoCalculateTarget;
    private Double salaryP1;
    private Double salaryP2;
    private Double salaryP3;
    private Double calculatedRevenueTarget;
    private String currency;
    private Boolean active;
    private String note;
}
