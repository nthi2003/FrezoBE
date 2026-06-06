package com.frezo.email.service;


import com.frezo.email.dto.request.EmailTemplateFilter;
import com.frezo.email.dto.request.EmailTemplateRequest;
import com.frezo.email.dto.response.EmailTemplateResponse;
import com.frezo.util.web.Response;

import java.util.Map;

public interface EmailtemplateService {
    Response<?> add(EmailTemplateRequest request);
    Response<?> edit(String id, EmailTemplateRequest request);
    EmailTemplateResponse view (String id);
    void delete (String id);
    Map<String, Object> all (EmailTemplateFilter filter);
}
