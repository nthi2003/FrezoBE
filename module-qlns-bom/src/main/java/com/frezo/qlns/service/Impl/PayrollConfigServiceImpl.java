package com.frezo.qlns.service.Impl;

import com.frezo.qlns.dto.request.PayrollConfigRequest;
import com.frezo.qlns.dto.response.PayrollConfigResponse;
import com.frezo.qlns.entity.PayrollConfig;
import com.frezo.qlns.mapper.PayrollConfigMapper;
import com.frezo.qlns.repository.PayrollConfigRepository;
import com.frezo.qlns.service.PayrollConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PayrollConfigServiceImpl implements PayrollConfigService {

    private final PayrollConfigRepository payrollConfigRepository;
    private final PayrollConfigMapper payrollConfigMapper;

    @Override
    @Transactional
    public PayrollConfigResponse save(PayrollConfigRequest request) {
        var existing = payrollConfigRepository.findByOrgIdAndYearAndIsActiveTrue(request.getOrgId(), request.getYear());
        PayrollConfig entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setStandardWorkingDays(request.getStandardWorkingDays());
            entity.setStandardHoursPerDay(request.getStandardHoursPerDay());
            entity.setOvertimeNormalRate(request.getOvertimeNormalRate());
            entity.setOvertimeWeekendRate(request.getOvertimeWeekendRate());
            entity.setOvertimeHolidayRate(request.getOvertimeHolidayRate());
            entity.setOvertimeNightRate(request.getOvertimeNightRate());
            entity.setLatePenaltyPerMinute(request.getLatePenaltyPerMinute());
            entity.setUnionDueRate(request.getUnionDueRate());
            entity.setMaxUnionDue(request.getMaxUnionDue());
            entity.setIsActive(request.getIsActive());
        } else {
            entity = payrollConfigMapper.toEntity(request);
        }
        return payrollConfigMapper.toResponse(payrollConfigRepository.save(entity));
    }

    @Override
    public PayrollConfigResponse getByOrgAndYear(String orgId, Integer year) {
        return payrollConfigRepository.findByOrgIdAndYearAndIsActiveTrue(orgId, year)
                .map(payrollConfigMapper::toResponse)
                .orElse(null);
    }
}
