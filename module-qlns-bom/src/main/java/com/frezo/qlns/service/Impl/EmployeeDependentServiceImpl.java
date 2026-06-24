package com.frezo.qlns.service.Impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.qlns.dto.request.EmployeeDependentRequest;
import com.frezo.qlns.dto.response.EmployeeDependentResponse;
import com.frezo.qlns.entity.EmployeeDependent;
import com.frezo.qlns.mapper.EmployeeDependentMapper;
import com.frezo.qlns.repository.EmployeeDependentRepository;
import com.frezo.qlns.service.EmployeeDependentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EmployeeDependentServiceImpl implements EmployeeDependentService {

    private final EmployeeDependentRepository employeeDependentRepository;
    private final EmployeeDependentMapper employeeDependentMapper;

    @Override
    @Transactional
    public EmployeeDependentResponse create(EmployeeDependentRequest request) {
        EmployeeDependent entity = employeeDependentMapper.toEntity(request);
        return employeeDependentMapper.toResponse(employeeDependentRepository.save(entity));
    }

    @Override
    @Transactional
    public EmployeeDependentResponse update(String id, EmployeeDependentRequest request) {
        EmployeeDependent entity = employeeDependentRepository.findById(id)
                .orElseThrow(() -> new QTHTException("Không tìm thấy người phụ thuộc"));
        entity.setFullName(request.getFullName());
        entity.setRelationship(request.getRelationship());
        entity.setBirthDate(request.getBirthDate());
        entity.setTaxCode(request.getTaxCode());
        entity.setFromMonth(request.getFromMonth());
        entity.setFromYear(request.getFromYear());
        entity.setToMonth(request.getToMonth());
        entity.setToYear(request.getToYear());
        entity.setIsActive(request.getIsActive());
        return employeeDependentMapper.toResponse(employeeDependentRepository.save(entity));
    }

    @Override
    @Transactional
    public void delete(String id) {
        EmployeeDependent entity = employeeDependentRepository.findById(id)
                .orElseThrow(() -> new QTHTException("Không tìm thấy người phụ thuộc"));
        entity.setIsDeleted(true);
        employeeDependentRepository.save(entity);
    }

    @Override
    public EmployeeDependentResponse getById(String id) {
        return employeeDependentRepository.findById(id)
                .map(employeeDependentMapper::toResponse)
                .orElseThrow(() -> new QTHTException("Không tìm thấy người phụ thuộc"));
    }

    @Override
    public List<EmployeeDependentResponse> getByPersonId(String personId) {
        return employeeDependentRepository.findByPersonIdAndIsActiveTrue(personId)
                .stream().map(employeeDependentMapper::toResponse).toList();
    }
}
