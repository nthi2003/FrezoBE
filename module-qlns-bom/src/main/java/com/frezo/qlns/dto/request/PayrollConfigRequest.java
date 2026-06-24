package com.frezo.qlns.dto.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PayrollConfigRequest {
    private String orgId;
    private Integer year;
    private Integer standardWorkingDays;
    private Integer standardHoursPerDay;
    private BigDecimal overtimeNormalRate;
    private BigDecimal overtimeWeekendRate;
    private BigDecimal overtimeHolidayRate;
    private BigDecimal overtimeNightRate;
    private BigDecimal latePenaltyPerMinute;
    private BigDecimal unionDueRate;
    private BigDecimal maxUnionDue;
    private Boolean isActive;
}
