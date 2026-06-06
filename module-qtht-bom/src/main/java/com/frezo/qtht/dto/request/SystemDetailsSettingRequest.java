package com.frezo.qtht.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemDetailsSettingRequest {
    private AttendanceSettingRequest attendance;
    private PayrollSettingRequest payroll;
}
