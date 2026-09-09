package com.frezo.qlns.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.qlns.common.QlnsErrorCode;
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

    /**
     * Tạo mới một người phụ thuộc (giảm trừ gia cảnh) cho nhân viên.
     */
    @Override
    @Transactional
    public EmployeeDependentResponse create(EmployeeDependentRequest request) {
        EmployeeDependent entity = employeeDependentMapper.toEntity(request);
        return employeeDependentMapper.toResponse(employeeDependentRepository.save(entity));
    }

    /**
     * Cập nhật thông tin người phụ thuộc theo id (thông tin, thời hạn, trạng thái hoạt động).
     */
    @Override
    @Transactional
    public EmployeeDependentResponse update(String id, EmployeeDependentRequest request) {
        EmployeeDependent entity = employeeDependentRepository.findById(id)
                .orElseThrow(() -> new AppException(QlnsErrorCode.DEPENDENT_NOT_FOUND));
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

    /**
     * Xoá mềm (soft-delete) một người phụ thuộc theo id.
     */
    @Override
    @Transactional
    public void delete(String id) {
        EmployeeDependent entity = employeeDependentRepository.findById(id)
                .orElseThrow(() -> new AppException(QlnsErrorCode.DEPENDENT_NOT_FOUND));
        entity.setIsDeleted(true);
        employeeDependentRepository.save(entity);
    }

    /**
     * Lấy chi tiết một người phụ thuộc theo id, không tồn tại thì báo lỗi DEPENDENT_NOT_FOUND.
     */
    @Override
    public EmployeeDependentResponse getById(String id) {
        return employeeDependentRepository.findById(id)
                .map(employeeDependentMapper::toResponse)
                .orElseThrow(() -> new AppException(QlnsErrorCode.DEPENDENT_NOT_FOUND));
    }

    /**
     * Lấy danh sách người phụ thuộc đang hoạt động của một nhân viên theo personId.
     */
    @Override
    public List<EmployeeDependentResponse> getByPersonId(String personId) {
        return employeeDependentRepository.findByPersonIdAndIsActiveTrue(personId)
                .stream().map(employeeDependentMapper::toResponse).toList();
    }
}
