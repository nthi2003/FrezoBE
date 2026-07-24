package com.frezo.email.service;


import com.frezo.email.dto.request.EmailTemplateFilter;
import com.frezo.email.dto.request.EmailTemplateRequest;
import com.frezo.email.dto.response.EmailTemplateResponse;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.PageResponse;

public interface EmailtemplateService {
    ApiResponse<?> add(EmailTemplateRequest request);
    ApiResponse<?> edit(String id, EmailTemplateRequest request);
    EmailTemplateResponse view (String id);
    void delete (String id);
    PageResponse<EmailTemplateResponse> all(EmailTemplateFilter filter);
}
