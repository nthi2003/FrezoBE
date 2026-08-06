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
import com.frezo.qtht.dto.request.PersonImportBatchRequest;
import com.frezo.qtht.dto.request.PersonImportRowRequest;
import com.frezo.qtht.dto.response.PersonImportResultResponse;
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
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

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

    @Override
    @Transactional(readOnly = true)
    public List<PersonResponse> exportAll(PersonFilterRequest filter) {
        PersonFilterRequest f = filter != null ? filter : new PersonFilterRequest();
        f.setPageNumber(1);
        f.setPageSize(10000);
        PageResponse<PersonResponse> page = all(f);
        return page.getItems() != null ? page.getItems() : List.of();
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public PersonImportResultResponse importBatch(PersonImportBatchRequest request) {
        List<PersonImportRowRequest> rows = request.getRows() != null ? request.getRows() : List.of();
        List<String> errors = new ArrayList<>();
        int success = 0;
        for (int i = 0; i < rows.size(); i++) {
            PersonImportRowRequest row = rows.get(i);
            int line = i + 1;
            try {
                if (row.getEmail() == null || row.getEmail().trim().isEmpty()) {
                    throw new IllegalArgumentException("Email bắt buộc");
                }
                if (row.getName() == null || row.getName().trim().isEmpty()) {
                    throw new IllegalArgumentException("Họ tên bắt buộc");
                }
                String code = row.getCode();
                if (code == null || code.trim().isEmpty()) {
                    code = "NV" + (System.currentTimeMillis() % 100000) + line;
                }
                PersonAddRequest add = new PersonAddRequest();
                add.setCode(code.trim());
                add.setName(row.getName().trim());
                add.setEmail(row.getEmail().trim());
                add.setPhone(trimOrNull(row.getPhone()));
                add.setGender(trimOrNull(row.getGender()));
                add.setAddress(trimOrNull(row.getAddress()));
                add.setOrgId(trimOrNull(row.getOrgId()));
                add.setDepartmentId(trimOrNull(row.getDepartmentId()));
                add.setJobTitle(trimOrNull(row.getJobTitle()));
                add.setIdentityNumber(trimOrNull(row.getIdentityNumber()));
                add.setSocialInsuranceNumber(trimOrNull(row.getSocialInsuranceNumber()));
                add.setBankAccount(trimOrNull(row.getBankAccount()));
                add.setBankName(trimOrNull(row.getBankName()));
                add.setActivated(true);
                add.setDob(parseDate(row.getBirthDate()));
                add.setJoinDate(parseDate(row.getJoinDate()));
                createPerson(add);
                success++;
            } catch (Exception ex) {
                errors.add("Dòng " + line + ": " + ex.getMessage());
            }
        }
        return PersonImportResultResponse.builder()
                .total(rows.size())
                .success(success)
                .failed(rows.size() - success)
                .errors(errors)
                .build();
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
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
        person.setIdentityNumber(trimOrNull(request.getIdentityNumber()));
        person.setSocialInsuranceNumber(trimOrNull(request.getSocialInsuranceNumber()));
        person.setBankAccount(trimOrNull(request.getBankAccount()));
        person.setBankName(trimOrNull(request.getBankName()));
        person.setBankBranch(trimOrNull(request.getBankBranch()));
        person.setJoinDate(request.getJoinDate());
        person.setResignDate(request.getResignDate());
        person.setJobPositionId(trimOrNull(request.getJobPositionId()));
        person.setIdCardFrontUrl(request.getIdCardFrontUrl());
        person.setIdCardBackUrl(request.getIdCardBackUrl());
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
