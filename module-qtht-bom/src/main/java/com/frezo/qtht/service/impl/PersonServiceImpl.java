package com.frezo.qtht.service.impl;

import com.frezo.auth.entity.User;
import com.frezo.auth.repository.UserRepository;
import com.frezo.common.exception.AppException;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qtht.constant.QthtErrorCode;
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
import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.ComboboxResponse;
import com.frezo.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.frezo.qtht.entity.Department;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;
    private final OrganizationRepository organizationRepository;
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
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
    public PageResponse<PersonResponse> all(PersonFilterRequest filter) {
        Specification<Person> specification = createSpecification(filter);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate");

        Page<Person> entities = personRepository.findAll(
                specification,
                ServiceHelper.createPageable(filter.getPageNumber(), filter.getPageSize(), sort));

        return PageResponse.from(entities, personMapper::toResponse);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ApiResponse<PersonResponse> createPerson(PersonAddRequest request) {
        validateCodeUniqueness(request.getCode());

        Person person = buildPersonFromRequest(request);
        Person savedPerson = personRepository.save(person);

        PersonResponse response = personMapper.toResponse(savedPerson);
        return ApiResponse.ok(response);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public ApiResponse<PersonResponse> updatePerson(String id, PersonUpdateRequest request) {
        Person person = findPersonById(id);

        personMapper.updateEntity(person, request);
        setOrganization(person, request.getOrgId());
        setDepartment(person, request.getDepartmentId());

        Person saved = personRepository.save(person);
        return ApiResponse.ok(personMapper.toResponse(saved));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAdmin(String id) {
        return personRepository.findById(id)
                .map(Person::getIsAdmin)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public PersonResponse getById(String id) {
        Person person = findPersonById(id);
        return personMapper.toResponse(person);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComboboxResponse> getCombobox(PersonFilterRequest filter) {
        return getCombobox(filter, "id");
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComboboxResponse> getCombobox(PersonFilterRequest filter, String valueField) {
        PageResponse<PersonResponse> data = all(filter);
        List<PersonResponse> items = data.getItems() != null ? data.getItems() : List.of();
        boolean useUsername = "username".equalsIgnoreCase(valueField);
        List<ComboboxResponse> out = new ArrayList<>(items.size());
        for (PersonResponse p : items) {
            String username = null;
            if (useUsername) {
                username = userRepository.findByPersonId(p.getId())
                        .map(User::getUserName)
                        .orElse(null);
                if (!StringUtils.hasText(username)) {
                    continue;
                }
            }
            String value = useUsername ? username.trim() : p.getId();
            String label = useUsername
                    ? p.getName() + " (" + username.trim() + ")"
                    : p.getName() + " (" + p.getCode() + ")";
            String description = (p.getJobTitle() != null ? p.getJobTitle() : "")
                    + (p.getEmail() != null ? " - " + p.getEmail() : "");
            out.add(ComboboxResponse.builder()
                    .value(value)
                    .label(label)
                    .description(description.trim().isEmpty() ? null : description)
                    .build());
        }
        return out;
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
                .orElseThrow(() -> new AppException(QthtErrorCode.PERSON_NOT_FOUND));
    }

    private void validateCodeUniqueness(String code) {
        if (code != null && personRepository.existsByCode(code)) {
            throw new AppException(QthtErrorCode.PERSON_CODE_EXISTS, code);
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
                    .orElseThrow(() -> new AppException(QthtErrorCode.ORGANIZATION_NOT_FOUND));
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
                    .orElseThrow(() -> new AppException(QthtErrorCode.DEPARTMENT_NOT_FOUND));
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

        if (filter.getActivated() != null) {
            specification = specification
                    .and(GenericSpecification.booleanField("activated", filter.getActivated()));
        }

        if (SystemUtils.isNotNullOrEmpty(filter.getGender())) {
            specification = specification
                    .and(GenericSpecification.equalField("gender", filter.getGender()));
        }

        if (SystemUtils.isNotNullOrEmpty(filter.getJobTitle())) {
            specification = specification
                    .and(GenericSpecification.equalField("jobTitle", filter.getJobTitle()));
        }

        if (SystemUtils.isNotNullOrEmpty(filter.getOrgId())) {
            specification = specification
                    .and(GenericSpecification.equalField("orgId", filter.getOrgId()));
        }

        return specification;
    }
}
