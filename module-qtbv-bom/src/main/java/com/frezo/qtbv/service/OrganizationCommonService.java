package com.frezo.qtbv.service;

import com.frezo.qtbv.dto.response.OrganizationSimpleResponse;

import java.util.List;

public interface OrganizationCommonService {

    List<OrganizationSimpleResponse> getOrganizationsList();
}
