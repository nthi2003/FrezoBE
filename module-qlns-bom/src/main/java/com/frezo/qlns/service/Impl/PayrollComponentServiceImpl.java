package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.AppException;
import com.frezo.qlns.common.QlnsErrorCode;
import com.frezo.qlns.dto.request.PayrollComponentRequest;
import com.frezo.qlns.dto.response.PayrollComponentResponse;
import com.frezo.qlns.entity.PayrollComponent;
import com.frezo.qlns.repository.PayrollComponentRepository;
import com.frezo.qlns.service.PayrollComponentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollComponentServiceImpl implements PayrollComponentService {

    private final PayrollComponentRepository payrollComponentRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PayrollComponentResponse> list() {
        return payrollComponentRepository.findByIsDeletedFalseOrderByCodeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PayrollComponentResponse getById(String id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional
    public PayrollComponentResponse create(PayrollComponentRequest request) {
        validateCodeUnique(request.getCode(), null);
        PayrollComponent entity = PayrollComponent.builder()
                .code(request.getCode().trim().toUpperCase())
                .name(request.getName().trim())
                .nature(request.getNature())
                .taxableType(request.getTaxableType())
                .taxDeductible(request.getTaxDeductible())
                .quotaType(request.getQuotaType())
                .quotaValue(request.getQuotaValue())
                .defaultValue(request.getDefaultValue())
                .activated(request.getActivated() != null ? request.getActivated() : true)
                .build();
        entity.setIsDeleted(false);
        return toResponse(payrollComponentRepository.save(entity));
    }

    @Override
    @Transactional
    public PayrollComponentResponse update(String id, PayrollComponentRequest request) {
        PayrollComponent entity = findOrThrow(id);
        if (request.getCode() != null) {
            validateCodeUnique(request.getCode(), id);
            entity.setCode(request.getCode().trim().toUpperCase());
        }
        if (request.getName() != null) entity.setName(request.getName().trim());
        if (request.getNature() != null) entity.setNature(request.getNature());
        if (request.getTaxableType() != null) entity.setTaxableType(request.getTaxableType());
        if (request.getTaxDeductible() != null) entity.setTaxDeductible(request.getTaxDeductible());
        if (request.getQuotaType() != null) entity.setQuotaType(request.getQuotaType());
        if (request.getQuotaValue() != null) entity.setQuotaValue(request.getQuotaValue());
        if (request.getDefaultValue() != null) entity.setDefaultValue(request.getDefaultValue());
        if (request.getActivated() != null) entity.setActivated(request.getActivated());
        return toResponse(payrollComponentRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(String id) {
        PayrollComponent entity = findOrThrow(id);
        entity.setIsDeleted(true);
        payrollComponentRepository.save(entity);
    }

    private void validateCodeUnique(String code, String excludeId) {
        boolean exists = excludeId == null
                ? payrollComponentRepository.existsByCodeAndIsDeletedFalse(code.trim().toUpperCase())
                : payrollComponentRepository.existsByCodeAndIsDeletedFalseAndIdNot(code.trim().toUpperCase(), excludeId);
        if (exists) {
            throw new AppException(QlnsErrorCode.CODE_EXISTS, code);
        }
    }

    private PayrollComponent findOrThrow(String id) {
        return payrollComponentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(QlnsErrorCode.ENTITY_NOT_FOUND));
    }

    private PayrollComponentResponse toResponse(PayrollComponent e) {
        return PayrollComponentResponse.builder()
                .id(e.getId())
                .code(e.getCode())
                .name(e.getName())
                .nature(e.getNature())
                .taxableType(e.getTaxableType())
                .taxDeductible(e.getTaxDeductible())
                .quotaType(e.getQuotaType())
                .quotaValue(e.getQuotaValue())
                .defaultValue(e.getDefaultValue())
                .activated(e.getActivated())
                .build();
    }
}
