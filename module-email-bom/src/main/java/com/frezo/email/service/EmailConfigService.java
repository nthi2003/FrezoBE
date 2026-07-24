package com.frezo.email.service;

import com.frezo.email.dto.request.EmailConfigAddRequest;
import com.frezo.email.dto.request.EmailConfigEditRequest;
import com.frezo.email.dto.request.EmailConfigFilter;
import com.frezo.email.dto.response.EmailConfigResponse;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.PageResponse;

public interface EmailConfigService {
    void deactivate(String id);

    void activate(String id);

    ApiResponse<?> edit(String id, EmailConfigEditRequest request);

    PageResponse<EmailConfigResponse> all(EmailConfigFilter filter);

    ApiResponse<?> add(EmailConfigAddRequest request);

    void delete(String id);

    void testConnection(String id);
}
