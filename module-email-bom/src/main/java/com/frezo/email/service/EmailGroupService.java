package com.frezo.email.service;

import com.frezo.email.dto.request.EmailGroupRequest;
import com.frezo.email.dto.response.EmailGroupResponse;
import com.frezo.common.response.ApiResponse;

import java.util.List;

public interface EmailGroupService {
    List<EmailGroupResponse> getAll();
    EmailGroupResponse getById(String id);
    ApiResponse<EmailGroupResponse> create(EmailGroupRequest request);
    ApiResponse<EmailGroupResponse> update(String id, EmailGroupRequest request);
    void delete(String id);
}
