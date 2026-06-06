package com.frezo.qtbv.service.impl;

import com.frezo.qtbv.dto.response.OrganizationSimpleResponse;
import com.frezo.qtbv.service.OrganizationCommonService;
import com.frezo.qtht.entity.Organization;
import com.frezo.qtht.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationCommonServiceImpl implements OrganizationCommonService {

    private final OrganizationRepository organizationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationSimpleResponse> getOrganizationsList() {
        return organizationRepository.findAll().stream()
                .map(this::toOrganizationResponse)
                .toList();
    }

    private OrganizationSimpleResponse toOrganizationResponse(Organization organization) {
        if (organization == null) {
            return null;
        }

        return OrganizationSimpleResponse.builder()
                .id(organization.getId())
                .code(organization.getCode())
                .name(organization.getName())
                .shortName(organization.getShortName())
                .email(organization.getEmail())
                .phone(organization.getPhone())
                .status(organization.getStatus())
                .build();
    }
}
