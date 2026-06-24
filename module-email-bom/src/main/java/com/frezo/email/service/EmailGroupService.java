package com.frezo.email.service;

import com.frezo.email.dto.request.EmailGroupRequest;
import com.frezo.email.dto.response.EmailGroupResponse;
import com.frezo.util.web.Response;

import java.util.List;

public interface EmailGroupService {
    List<EmailGroupResponse> getAll();
    EmailGroupResponse getById(String id);
    Response<EmailGroupResponse> create(EmailGroupRequest request);
    Response<EmailGroupResponse> update(String id, EmailGroupRequest request);
    void delete(String id);
}
