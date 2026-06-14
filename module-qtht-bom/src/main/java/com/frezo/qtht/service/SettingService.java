package com.frezo.qtht.service;

import com.frezo.common.response.ApiResponse;
import com.frezo.qtht.dto.request.SettingAddRequest;
import com.frezo.qtht.dto.request.SettingEditRequest;
import com.frezo.qtht.dto.response.AttendanceSettingResponse;
import com.frezo.qtht.dto.response.PayrollSettingResponse;
import com.frezo.qtht.dto.response.SettingResponse;
import java.util.List;

public interface SettingService {
    List<SettingResponse> findAll();
    ApiResponse<SettingResponse> add(SettingAddRequest request);
    ApiResponse<SettingResponse> edit(SettingEditRequest request);
    ApiResponse<SettingResponse> getByOrgId(String orgId);

    PayrollSettingResponse getPayrollSetting(String orgId);
    AttendanceSettingResponse getAttendanceSetting(String orgId);
}
