package com.frezo.qtht.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qtht.dto.request.PersonAddRequest;
import com.frezo.qtht.dto.request.PersonFilterRequest;
import com.frezo.qtht.dto.request.PersonUpdateRequest;
import com.frezo.qtht.dto.response.PersonResponse;
import com.frezo.qtht.entity.Organization;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.mapper.PersonMapper;
import com.frezo.qtht.repository.DepartmentRepository;
import com.frezo.qtht.repository.OrganizationRepository;
import com.frezo.qtht.repository.PersonRepository;
import com.frezo.qtht.service.PersonService;
import com.frezo.util.web.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.frezo.qtht.entity.Department;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    private static final String ERROR_CODE_EXISTS = "exception.person.code.exists";
    private static final String ERROR_PERSON_NOT_FOUND = "invalid.person.entity.not.found";
    private static final String ERROR_ORG_NOT_FOUND = "invalid.organization.entity.not.found";

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;
    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final com.frezo.common.service.MinioService minioService;

    @Override
    public String uploadAvatarTemp(String userName, org.springframework.web.multipart.MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".png";
        
        String objectName = userName + "/avatar_temp" + extension;
        return minioService.uploadFile(objectName, file);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> all(PersonFilterRequest filter) {
        Specification<Person> specification = createSpecification(filter);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate");

        Page<Person> entities = personRepository.findAll(
                specification,
                ServiceHelper.createPageable(filter.getPageNumber(), filter.getPageSize(), sort));

        List<PersonResponse> responses = entities.getContent().stream()
                .map(personMapper::toResponse)
                .toList();

        return ServiceHelper.createResponse1(filter.getPageNumber(), filter.getPageSize(), entities, responses);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Response<PersonResponse> createPerson(PersonAddRequest request) {
        validateCodeUniqueness(request.getCode());

        Person person = buildPersonFromRequest(request);
        Person savedPerson = personRepository.save(person);

        PersonResponse response = personMapper.toResponse(savedPerson);
        return Response.ok(response);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Response<PersonResponse> updatePerson(String id, PersonUpdateRequest request) {
        Person person = findPersonById(id);

        personMapper.updateEntity(person, request);
        setOrganization(person, request.getOrgId());
        setDepartment(person, request.getDepartmentId());

        Person saved = personRepository.save(person);
        return Response.ok(personMapper.toResponse(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAdmin(String id) {
        return personRepository.findById(id)
                .map(Person::getIsAdmin)
                .orElse(false);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void delete(String id) {
        Person person = findPersonById(id);
        person.setIsDeleted(true);
        personRepository.save(person);
    }

    @Override
    @Transactional
    public void activate(String id) {
        Person person = findPersonById(id);
        person.setActivated(true);
        personRepository.save(person);
    }

    @Override
    @Transactional
    public void deactivate(String id) {
        Person person = findPersonById(id);
        person.setActivated(false);
        personRepository.save(person);
    }

    @Transactional(readOnly = true)
    protected Person findPersonById(String id) {
        return personRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new QTHTException(ERROR_PERSON_NOT_FOUND));
    }

    private void validateCodeUniqueness(String code) {
        if (code != null && personRepository.existsByCode(code)) {
            throw new QTHTException(ERROR_CODE_EXISTS, code);
        }
    }

    private Person buildPersonFromRequest(PersonAddRequest request) {
        Person person = new Person();
        person.setId(UUID.randomUUID().toString());
        person.setCode(request.getCode());
        person.setName(request.getName());
        person.setShortName(trimOrNull(request.getShortName()));
        person.setEmail(request.getEmail());
        person.setPhone(request.getPhone());
        person.setDob(request.getDob());
        person.setGender(request.getGender());
        person.setAddress(request.getAddress());
        person.setDescription(request.getDescription());
        person.setIsAdmin(request.getIsAdmin());
        person.setActivated(request.getActivated());
        person.setJobTitle(trimOrNull(request.getJobTitle()));
        person.setAvatarUrl(request.getAvatarUrl());
        person.setIsDeleted(false);

        setOrganization(person, request.getOrgId());
        setDepartment(person, request.getDepartmentId());

        return person;
    }

    private void setOrganization(Person person, String orgId) {
        if (orgId != null && !orgId.trim().isEmpty()) {
            Organization organization = organizationRepository.findById(orgId.trim())
                    .orElseThrow(() -> new QTHTException(ERROR_ORG_NOT_FOUND));
            person.setOrganization(organization);
            log.debug("Set organization: id={}, name={}", organization.getId(), organization.getName());
        } else {
            person.setOrganization(null);
            log.debug("Organization ID is null or empty, set organization to null");
        }
    }

    private void setDepartment(Person person, String departmentId) {
        if (departmentId != null && !departmentId.trim().isEmpty()) {
            Department department = departmentRepository.findById(departmentId.trim())
                    .orElseThrow(() -> new QTHTException("invalid.department.entity.not.found"));
            person.setDepartment(department);
        } else {
            person.setDepartment(null);
        }
    }

    private String trimOrNull(String value) {
        return (value != null && !value.trim().isEmpty()) ? value.trim() : null;
    }

    private Specification<Person> createSpecification(PersonFilterRequest filter) {
        Specification<Person> specification = Specification
                .where(GenericSpecification.hasFieldIs("isDeleted", Boolean.FALSE));

        if (SystemUtils.isNotNullOrEmpty(filter.getKeyword())) {
            specification = specification.and(
                    GenericSpecification.<Person>likeField("name", filter.getKeyword())
                            .or(GenericSpecification.likeField("code", filter.getKeyword()))
                            .or(GenericSpecification.likeField("shortName", filter.getKeyword())));
        }

        if (SystemUtils.isNotNullOrEmpty(filter.getDepartmentId())) {
            specification = specification
                    .and(GenericSpecification.equalField("departmentId", filter.getDepartmentId()));
        }

        return specification;
    }
}
