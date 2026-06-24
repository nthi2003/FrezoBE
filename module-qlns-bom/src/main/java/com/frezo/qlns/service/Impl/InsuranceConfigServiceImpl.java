package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.qlns.dto.request.InsuranceConfigRequest;
import com.frezo.qlns.dto.response.InsuranceConfigResponse;
import com.frezo.qlns.entity.InsuranceConfig;
import com.frezo.qlns.mapper.InsuranceConfigMapper;
import com.frezo.qlns.repository.InsuranceConfigRepository;
import com.frezo.qlns.service.InsuranceConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InsuranceConfigServiceImpl implements InsuranceConfigService {

    private final InsuranceConfigRepository insuranceConfigRepository;
    private final InsuranceConfigMapper insuranceConfigMapper;

    @Override
    @Transactional
    public InsuranceConfigResponse save(InsuranceConfigRequest request) {
        var existing = insuranceConfigRepository.findByYearAndIsActiveTrue(request.getYear());
        InsuranceConfig entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setSocialInsuranceRate(request.getSocialInsuranceRate());
            entity.setHealthInsuranceRate(request.getHealthInsuranceRate());
            entity.setUnemploymentInsuranceRate(request.getUnemploymentInsuranceRate());
            entity.setEmployerSocialRate(request.getEmployerSocialRate());
            entity.setEmployerHealthRate(request.getEmployerHealthRate());
            entity.setEmployerUnemploymentRate(request.getEmployerUnemploymentRate());
            entity.setEmployerAccidentRate(request.getEmployerAccidentRate());
            entity.setMaxInsuranceSalary(request.getMaxInsuranceSalary());
            entity.setBaseMinimumWage(request.getBaseMinimumWage());
            entity.setRegionalMinimumWage(request.getRegionalMinimumWage());
            entity.setRegion(request.getRegion());
            entity.setAppliesFrom(request.getAppliesFrom());
            entity.setAppliesTo(request.getAppliesTo());
            entity.setIsActive(request.getIsActive());
        } else {
            entity = insuranceConfigMapper.toEntity(request);
        }
        return insuranceConfigMapper.toResponse(insuranceConfigRepository.save(entity));
    }

    @Override
    public InsuranceConfigResponse getByYear(Integer year) {
        return insuranceConfigRepository.findByYearAndIsActiveTrue(year)
                .map(insuranceConfigMapper::toResponse)
                .orElseThrow(() -> new QTHTException("Không tìm thấy cấu hình bảo hiểm cho năm " + year));
    }
}
