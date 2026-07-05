package com.frezo.fbautomation.service;

import com.frezo.fbautomation.dto.response.FacebookLeadResponse;

import java.util.List;

public interface FacebookLeadService {

    List<FacebookLeadResponse> getAll(String status);

    FacebookLeadResponse getById(String id);

    void delete(String id);

    String importToCustomer(String id);

    int importAllToCustomer(List<String> ids);

    long countByStatus(String status);
}
