package com.frezo.qtht.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemDetailsSettingResponse {
    private AttendanceSettingResponse attendance;
    private PayrollSettingResponse payroll;
}
