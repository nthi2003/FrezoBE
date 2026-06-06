package com.frezo.qtht.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollSettingRequest {
    private Integer calculationStartDay; 
    private Integer standardWorkingDays; 
    private Long latePenaltyPerMinute; 
    private Long overtimePayPerMinute; 
    private Boolean isAutoGeneratePayroll; 
    private Boolean isAutoUpdatePayroll; 
    private String revenueType; 
}
