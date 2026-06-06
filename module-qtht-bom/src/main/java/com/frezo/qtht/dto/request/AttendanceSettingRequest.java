package com.frezo.qtht.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceSettingRequest {
    private Integer standardHours; 
    private Double halfDayThreshold; 
    private Integer lateThreshold; 
    private Integer earlyThreshold; 
    private Integer overtimeBeforeThreshold; 
    private Integer overtimeAfterThreshold; 
    private Boolean isAutoAttendance; 
    private Integer maxShiftsPerDay; 
    private Integer minGapBetweenShifts; 
}
