package com.frezo.qtht.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qtht.common.DepartmentStatus;
import com.frezo.qtht.dto.request.DepartmentFilterRequest;
import com.frezo.qtht.dto.response.DepartmentResponse;
import com.frezo.qtht.entity.Department;
import com.frezo.qtht.mapper.DepartmentMapper;
import com.frezo.qtht.repository.DepartmentRepository;
import com.frezo.qtht.repository.OrganizationRepository;
import com.frezo.qtht.repository.PersonRepository;
import com.frezo.qtht.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private static final String ERROR_DEPT_NOT_FOUND = "invalid.department.entity.not.found";
    private static final String ERROR_CODE_EXISTS = "department.code.already.exists";

    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;
    private final PersonRepository personRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> all(DepartmentFilterRequest filter) {
        try {
            Specification<Department> specification = createSpecification(filter);
            Sort sort = Sort.by(Sort.Direction.DESC, "createdDate");
            Page<Department> entities = departmentRepository.findAll(
                    specification, ServiceHelper.createPageable(filter.getPageNumber(), filter.getPageSize(), sort));

            List<DepartmentResponse> response = entities.getContent().stream()
                    .map(departmentMapper::toResponse)
                    .toList();

            return ServiceHelper.createResponse1(filter.getPageNumber(), filter.getPageSize(), entities, response);
        } catch (Exception e) {
            throw e;
        }
    }

    @Override
    @Transactional
    public void delete(String id) {
        Department department = findEntityById(id);
        department.setIsDeleted(true);
        departmentRepository.save(department);
        log.debug("Deleted department: {}", id);
    }

    @Transactional(readOnly = true)
    private Department findEntityById(String id) {
        return departmentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new QTHTException(ERROR_DEPT_NOT_FOUND));
    }

    private Specification<Department> createSpecification(DepartmentFilterRequest filter) {

        Specification<Department> specification = Specification
                .where(GenericSpecification.hasFieldIs("isDeleted", Boolean.FALSE));

        if (SystemUtils.isNotNullOrEmpty(filter.getKeyword())) {
            specification = specification.and(
                    GenericSpecification.<Department>equalField("name", filter.getKeyword())
                            .or(GenericSpecification.equalField("code", filter.getKeyword())));
        }

        if (SystemUtils.isNotNullOrEmpty(filter.getOrganizationId())) {
            specification = specification
                    .and(GenericSpecification.equalField("organizationId", filter.getOrganizationId()));
        }
        return specification;
    }

    @Override
    @Transactional
    public DepartmentResponse create(com.frezo.qtht.dto.request.DepartmentSaveRequest request) {
        if (departmentRepository.existsByCode(request.getCode())) {
            throw new QTHTException(ERROR_CODE_EXISTS, request.getCode());
        }

        Department department = departmentMapper.toEntity(request);
        department.setId(java.util.UUID.randomUUID().toString());
        department.setLevel(1);
        department.setStatus(DepartmentStatus.ACTIVE);
        department.setIsDeleted(false);

        // Set Relationships
        if (SystemUtils.isNotNullOrEmpty(request.getOrganizationId())) {
            organizationRepository.findById(request.getOrganizationId())
                .ifPresent(department::setOrganization);
        }
        if (SystemUtils.isNotNullOrEmpty(request.getParentId())) {
            departmentRepository.findById(request.getParentId())
                .ifPresent(department::setParent);
        }
        if (SystemUtils.isNotNullOrEmpty(request.getManagerId())) {
            personRepository.findById(request.getManagerId())
                .ifPresent(department::setManager);
        }
        if (SystemUtils.isNotNullOrEmpty(request.getDeputyManagerId())) {
            personRepository.findById(request.getDeputyManagerId())
                .ifPresent(department::setDeputyManager);
        }

        Department saved = departmentRepository.save(department);
        return departmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DepartmentResponse update(String id, com.frezo.qtht.dto.request.DepartmentSaveRequest request) {
        Department department = findEntityById(id);

        departmentMapper.updateEntity(request, department);

        // Update Relationships
        if (SystemUtils.isNotNullOrEmpty(request.getOrganizationId())) {
            organizationRepository.findById(request.getOrganizationId())
                .ifPresentOrElse(department::setOrganization, () -> department.setOrganization(null));
        } else {
            department.setOrganization(null);
        }

        if (SystemUtils.isNotNullOrEmpty(request.getParentId())) {
            departmentRepository.findById(request.getParentId())
                .ifPresentOrElse(department::setParent, () -> department.setParent(null));
        } else {
            department.setParent(null);
        }

        if (SystemUtils.isNotNullOrEmpty(request.getManagerId())) {
            personRepository.findById(request.getManagerId())
                .ifPresentOrElse(department::setManager, () -> department.setManager(null));
        } else {
            department.setManager(null);
        }

        if (SystemUtils.isNotNullOrEmpty(request.getDeputyManagerId())) {
            personRepository.findById(request.getDeputyManagerId())
                .ifPresentOrElse(department::setDeputyManager, () -> department.setDeputyManager(null));
        } else {
            department.setDeputyManager(null);
        }

        Department saved = departmentRepository.save(department);
        return departmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void activate(String id) {
        Department department = findEntityById(id);
        department.setStatus(DepartmentStatus.ACTIVE);
        departmentRepository.save(department);
        log.debug("Activated department: {}", id);
    }

    @Override
    @Transactional
    public void deactivate(String id) {
        Department department = findEntityById(id);
        department.setStatus(DepartmentStatus.INACTIVE);
        departmentRepository.save(department);
        log.debug("Deactivated department: {}", id);
    }
}
