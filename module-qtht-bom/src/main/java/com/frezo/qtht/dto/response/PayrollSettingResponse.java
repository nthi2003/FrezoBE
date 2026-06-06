package com.frezo.qtht.dto.response;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollSettingResponse {
    private Integer calculationStartDay; 
    private Integer standardWorkingDays; 
    private Long latePenaltyPerMinute; 
    private Long overtimePayPerMinute; 
    private Boolean isAutoGeneratePayroll; 
    private Boolean isAutoUpdatePayroll; 
    private String revenueType; 
}
