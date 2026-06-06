package com.frezo.qtht.service;

import com.frezo.qtht.dto.request.OrganizationAddRequest;
import com.frezo.qtht.dto.request.OrganizationEditRequest;
import com.frezo.qtht.dto.request.OrganizationFilterRequest;
import com.frezo.qtht.dto.response.OrganizationResponse;
import com.frezo.qtht.dto.response.OrganizationDetailResponse;
import com.frezo.util.web.Response;

import java.util.List;
import java.util.Map;

public interface OrganizationService {
    Map<String, Object> all(OrganizationFilterRequest filter);

    Response<OrganizationResponse> add(OrganizationAddRequest request);

    Response<OrganizationResponse> update(String id, OrganizationEditRequest request);

    void delete(String id);
    Response<OrganizationDetailResponse> getById(String id);
    List<OrganizationResponse> getChildren(String parentId);
}
