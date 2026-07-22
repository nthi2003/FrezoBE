package com.frezo.qtht.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.response.PageResponse;
import com.frezo.qtht.common.DepartmentStatus;
import com.frezo.qtht.constant.QthtErrorCode;
import com.frezo.qtht.dto.request.DepartmentFilterRequest;
import com.frezo.qtht.dto.request.DepartmentSaveRequest;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Department service — <b>reference implementation cho Batch F</b> (chuẩn v1.1).
 * <p>
 * Áp dụng đầy đủ:
 * <ul>
 *   <li>Batch A — {@code AppException + QthtErrorCode}, {@code PageResponse<T>}, {@code PagingBase.toPageable(whitelist)}</li>
 *   <li>Batch D — {@code entity.softDelete(username)} thay set từng field</li>
 * </ul>
 * <p>
 * Các service khác migrate theo pattern này:
 * <ol>
 *   <li>Đổi return type {@code Map<String,Object>} → {@code PageResponse<XxxResponse>}</li>
 *   <li>Đổi {@code ServiceHelper.createResponse1(...)} → {@code PageResponse.from(page, mapper::toResponse)}</li>
 *   <li>Đổi {@code ServiceHelper.createPageable(...)} → {@code filter.toPageable(ALLOWED_SORT_FIELDS)}</li>
 *   <li>Đổi {@code QTHTException("magic.i18n.key")} → {@code AppException(XxxErrorCode.SPECIFIC_ERROR)}</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    /** Whitelist sort field — chống SQL injection từ query param {@code sortBy}. */
    private static final Set<String> ALLOWED_SORT_FIELDS =
            Set.of("createdDate", "updatedDate", "name", "code", "status", "level");

    private final DepartmentRepository departmentRepository;
    private final OrganizationRepository organizationRepository;
    private final PersonRepository personRepository;
    private final DepartmentMapper departmentMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> all(DepartmentFilterRequest filter) {
        Specification<Department> spec = createSpecification(filter);
        Pageable pageable = filter.toPageable(ALLOWED_SORT_FIELDS, "createdDate");
        Page<Department> page = departmentRepository.findAll(spec, pageable);
        return PageResponse.from(page, departmentMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DepartmentResponse> getTree() {
        List<Department> allDepts = departmentRepository.findAll(
                Specification.where(GenericSpecification.hasFieldIs("isDeleted", Boolean.FALSE))
        );
        List<DepartmentResponse> responses = allDepts.stream()
                .map(departmentMapper::toResponse)
                .toList();

        Map<String, DepartmentResponse> map = new HashMap<>();
        List<DepartmentResponse> roots = new ArrayList<>();

        for (DepartmentResponse dept : responses) {
            dept.setChildren(new ArrayList<>());
            map.put(dept.getId(), dept);
        }
        for (DepartmentResponse dept : responses) {
            if (dept.getParentId() != null && map.containsKey(dept.getParentId())) {
                map.get(dept.getParentId()).getChildren().add(dept);
            } else {
                roots.add(dept);
            }
        }
        return roots;
    }

    @Override
    @Transactional
    public void delete(String id) {
        Department department = findEntityById(id);
        department.softDelete(SystemUtils.getCurrentUsername());
        departmentRepository.save(department);
        log.info("Deleted department id={} by={}", id, department.getDeletedBy());
    }

    @Transactional(readOnly = true)
    private Department findEntityById(String id) {
        return departmentRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(QthtErrorCode.DEPARTMENT_NOT_FOUND, id));
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
    public DepartmentResponse create(DepartmentSaveRequest request) {
        if (departmentRepository.existsByCode(request.getCode())) {
            throw new AppException(QthtErrorCode.DEPARTMENT_CODE_EXISTS, request.getCode());
        }

        Department department = departmentMapper.toEntity(request);
        department.setLevel(1);
        department.setStatus(DepartmentStatus.ACTIVE);
        department.setIsDeleted(false);

        // Set relationships (id auto-gen trong BaseEntity.prePersist)
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
        log.info("Created department id={} code={}", saved.getId(), saved.getCode());
        return departmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public DepartmentResponse update(String id, DepartmentSaveRequest request) {
        Department department = findEntityById(id);
        departmentMapper.updateEntity(request, department);

        // Update relationships — null = remove
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
        log.info("Updated department id={} code={}", saved.getId(), saved.getCode());
        return departmentMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void activate(String id) {
        Department department = findEntityById(id);
        department.setStatus(DepartmentStatus.ACTIVE);
        departmentRepository.save(department);
        log.info("Activated department id={}", id);
    }

    @Override
    @Transactional
    public void deactivate(String id) {
        Department department = findEntityById(id);
        department.setStatus(DepartmentStatus.INACTIVE);
        departmentRepository.save(department);
        log.info("Deactivated department id={}", id);
    }
}
