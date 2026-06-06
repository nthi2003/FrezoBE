package com.frezo.qtht.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.qtht.dto.request.OrganizationAddRequest;
import com.frezo.qtht.dto.request.OrganizationEditRequest;
import com.frezo.qtht.dto.request.OrganizationFilterRequest;
import com.frezo.qtht.dto.response.OrganizationDetailResponse;
import com.frezo.qtht.dto.response.OrganizationResponse;
import com.frezo.qtht.entity.Organization;
import com.frezo.qtht.entity.Person;
import com.frezo.qtht.mapper.OrganizationMapper;
import com.frezo.qtht.repository.OrganizationRepository;
import com.frezo.qtht.repository.PersonRepository;
import com.frezo.qtht.service.OrganizationService;
import com.frezo.util.web.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationServiceImpl implements OrganizationService {

    private static final String ERROR_ORG_NOT_FOUND = "invalid.organization.entity.not.found";
    private static final String ERROR_PERSON_NOT_FOUND = "invalid.person.entity.not.found";
    private static final String ERROR_CODE_EXISTS = "organization.code.already.exists";
    private static final String ERROR_TAX_EXISTS = "organization.taxcode.already.exists";
    private static final String ERROR_PARENT_NOT_FOUND = "organization.parent.not.found";
    private static final String ERROR_LEVEL_INVALID = "organization.level.invalid";

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;
    private final PersonRepository personRepository;
    private final com.frezo.qtht.service.SettingService settingService;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> all(OrganizationFilterRequest filter) {
        Specification<Organization> specification = createSpecification(filter);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate");
        Page<Organization> entities = organizationRepository.findAll(
                specification, ServiceHelper.createPageable(filter.getPageNumber(), filter.getPageSize(), sort, true));

        List<OrganizationResponse> responses = entities.getContent().stream()
                .map(organizationMapper::toResponse)
                .toList();

        return ServiceHelper.createResponse1(filter.getPageNumber(), filter.getPageSize(), entities, responses);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<OrganizationResponse> add(OrganizationAddRequest request) {
        validateRequest(request);

        Organization organization = organizationMapper.toEntity(request);
        organization.setIsDeleted(false);

        processParentAndLevel(request.getParentId(), request.getLevel(), organization);
        updateLegalRepresentative(organization, request.getLegalRepresentativeId());

        Organization savedOrg = organizationRepository.save(organization);

        // Create default setting for the new organization
        com.frezo.qtht.dto.request.SettingAddRequest settingRequest = new com.frezo.qtht.dto.request.SettingAddRequest();
        settingRequest.setOrgId(savedOrg.getId());
        settingRequest.setIsAttendance(false);
        settingRequest.setIsColor(false);
        settingRequest.setIsEmail(false);
        settingRequest.setIsSwap(false);
        settingService.add(settingRequest);

        return Response.ok(organizationMapper.toResponse(savedOrg));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<OrganizationResponse> update(String id, OrganizationEditRequest request) {
        Organization organization = findEntityById(id);

        validateUpdate(organization, request);
        organizationMapper.updateEntity(request, organization);

        boolean parentChanged = isParentChanged(organization, request.getParentId());
        if (parentChanged) {
            processParentAndLevel(request.getParentId(), request.getLevel(), organization);
        }

        updateLegalRepresentative(organization, request.getLegalRepresentativeId());

        log.debug("Updating organization: {}", organization.getCode());
        Organization savedOrg = organizationRepository.save(organization);

        return Response.ok(organizationMapper.toResponse(savedOrg));
    }

    @Override
    @Transactional
    public void delete(String id) {
        Organization organization = findEntityById(id);
        organization.setIsDeleted(true);
        organizationRepository.save(organization);
        log.debug("Deleted organization: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public Response<OrganizationDetailResponse> getById(String id) {
        Organization organization = findEntityById(id);
        return Response.ok(organizationMapper.toDetailResponse(organization));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationResponse> getChildren(String parentId) {
        return Collections.emptyList();
    }



    @Transactional(readOnly = true)
    protected Organization findEntityById(String id) {
        return organizationRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new QTHTException(ERROR_ORG_NOT_FOUND));
    }

    private void validateRequest(OrganizationAddRequest request) {
        if (organizationRepository.existsByCode(request.getCode())) {
            throw new QTHTException(ERROR_CODE_EXISTS, request.getCode());
        }
        if (StringUtils.hasText(request.getTaxCode()) && organizationRepository.existsByTaxCode(request.getTaxCode())) {
            throw new QTHTException(ERROR_TAX_EXISTS, request.getTaxCode());
        }
    }

    private void validateUpdate(Organization organization, OrganizationEditRequest request) {
        if (!organization.getCode().equals(request.getCode())
                && organizationRepository.existsByCode(request.getCode())) {
            throw new QTHTException(ERROR_CODE_EXISTS, request.getCode());
        }

        if (StringUtils.hasText(request.getTaxCode()) && !request.getTaxCode().equals(organization.getTaxCode())
                && organizationRepository.existsByTaxCode(request.getTaxCode())) {
            throw new QTHTException(ERROR_TAX_EXISTS, request.getTaxCode());
        }
    }

    private void processParentAndLevel(String parentId, Integer level, Organization organization) {
        if (!StringUtils.hasText(organization.getId())) {
            organization.setId(UUID.randomUUID().toString());
        }

        if (StringUtils.hasText(parentId)) {
            Organization parent = organizationRepository.findById(parentId)
                    .orElseThrow(() -> new QTHTException(ERROR_PARENT_NOT_FOUND));

            if (level != null && level <= (parent.getLevel() != null ? parent.getLevel() : 0)) {
                throw new QTHTException(ERROR_LEVEL_INVALID);
            }

            organization.setParent(parent);
            organization.setParentId(parent.getId());
            String parentPath = StringUtils.hasText(parent.getPath()) ? parent.getPath() : "/";
            organization.setPath(parentPath + organization.getId() + "/");
        } else {
            organization.setParent(null);
            organization.setParentId(null);
            organization.setPath("/" + organization.getId() + "/");
        }
    }

    private void updateLegalRepresentative(Organization organization, String legalRepId) {
        if (StringUtils.hasText(legalRepId)) {
            Person legalRep = personRepository.findById(legalRepId)
                    .orElseThrow(() -> new QTHTException(ERROR_PERSON_NOT_FOUND));
            organization.setLegalRepresentative(legalRep);
        } else {
            organization.setLegalRepresentative(null);
        }
    }

    private boolean isParentChanged(Organization organization, String newParentId) {
        return (newParentId == null && organization.getParentId() != null)
                || (newParentId != null && !newParentId.equals(organization.getParentId()));
    }

    private Specification<Organization> createSpecification(OrganizationFilterRequest filter) {
        Specification<Organization> specification = Specification
                .where(GenericSpecification.hasFieldIs("isDeleted", Boolean.FALSE));
        if (SystemUtils.isNotNullOrEmpty(filter.getKeyword())) {
            specification = specification.and(
                    GenericSpecification.<Organization>likeField("name", filter.getKeyword())
                            .or(GenericSpecification.likeField("code", filter.getKeyword()))
                            .or(GenericSpecification.likeField("shortName", filter.getKeyword())));
        }
        return specification;
    }
}
