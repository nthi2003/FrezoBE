package com.frezo.qtht.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frezo.common.exception.QTHTException;
import com.frezo.common.response.ApiResponse;
import com.frezo.qtht.dto.request.SettingAddRequest;
import com.frezo.qtht.dto.request.SettingEditRequest;
import com.frezo.qtht.dto.response.AttendanceSettingResponse;
import com.frezo.qtht.dto.response.PayrollSettingResponse;
import com.frezo.qtht.dto.response.SettingResponse;
import com.frezo.qtht.dto.response.SystemDetailsSettingResponse;
import com.frezo.qtht.entity.Setting;
import com.frezo.qtht.mapper.SettingMapper;
import com.frezo.qtht.repository.SettingRepository;
import com.frezo.qtht.service.SettingService;
import com.frezo.util.web.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingServiceImpl implements SettingService {

    private final SettingRepository settingRepository;
    private final SettingMapper settingMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    @CacheEvict(value = "settings", key = "#request.orgId")
    public ApiResponse<SettingResponse> add(SettingAddRequest request) {
        if (settingRepository.findByOrgIdAndIsDeletedFalse(request.getOrgId()).isPresent()) {
            throw new QTHTException("setting.org.already.exists");
        }
        Setting setting = settingMapper.toEntity(request);
        Setting savedSetting = settingRepository.save(setting);
        return ApiResponse.success(settingMapper.toResponse(savedSetting));
    }

    @Override
    @Transactional
    @CacheEvict(value = "settings", key = "#request.orgId")
    public ApiResponse<SettingResponse> edit(SettingEditRequest request) {
        Setting setting = settingRepository.findByIdAndIsDeletedFalse(request.getId())
                .orElseThrow(() -> new QTHTException("setting.not.found"));

        settingMapper.updateEntity(setting, request);
        Setting savedSetting = settingRepository.save(setting);
        return ApiResponse.success(settingMapper.toResponse(savedSetting));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "settings", key = "#orgId")
    public ApiResponse<SettingResponse> getByOrgId(String orgId) {
        Setting setting = settingRepository.findByOrgIdAndIsDeletedFalse(orgId)
                .orElseGet(() -> Setting.builder()
                        .orgId(orgId)
                        .isAttendance(false)
                        .isColor(false)
                        .isEmail(false)
                        .isSwap(false)
                        .build());
        return ApiResponse.success(settingMapper.toResponse(setting));
    }

    @Override
    public PayrollSettingResponse getPayrollSetting(String orgId) {
        SystemDetailsSettingResponse details = getSystemDetails(orgId);
        if (details != null && details.getPayroll() != null) {
            return details.getPayroll();
        }
        // Default fallback values
        return PayrollSettingResponse.builder()
                .calculationStartDay(1)
                .standardWorkingDays(22)
                .latePenaltyPerMinute(10000L)
                .overtimePayPerMinute(20000L)
                .isAutoGeneratePayroll(false)
                .isAutoUpdatePayroll(false)
                .revenueType("NET")
                .build();
    }

    @Override
    public AttendanceSettingResponse getAttendanceSetting(String orgId) {
        SystemDetailsSettingResponse details = getSystemDetails(orgId);
        if (details != null && details.getAttendance() != null) {
            return details.getAttendance();
        }
        return AttendanceSettingResponse.builder()
                .standardHours(8)
                .halfDayThreshold(4.5)
                .lateThreshold(0)
                .earlyThreshold(0)
                .overtimeBeforeThreshold(0)
                .overtimeAfterThreshold(0)
                .isAutoAttendance(false)
                .maxShiftsPerDay(2)
                .minGapBetweenShifts(60)
                .build();
    }

    private SystemDetailsSettingResponse getSystemDetails(String orgId) {
        Setting setting = settingRepository.findByOrgIdAndIsDeletedFalse(orgId).orElse(null);
        if (setting == null || setting.getDetails() == null || setting.getDetails().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(setting.getDetails(), SystemDetailsSettingResponse.class);
        } catch (Exception e) {
            log.error("Error parsing system details for org {}: {}", orgId, e.getMessage());
            return null;
        }
    }
}
