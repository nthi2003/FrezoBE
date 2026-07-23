package com.frezo.email.service;

import com.frezo.email.dto.request.EmailConfigAddRequest;
import com.frezo.email.dto.request.EmailConfigEditRequest;
import com.frezo.email.dto.request.EmailConfigFilter;
import com.frezo.common.response.ApiResponse;

import java.util.Map;

public interface EmailConfigService {
    void deactivate(String id);

    void activate(String id);

    ApiResponse<?> edit(String id, EmailConfigEditRequest request);

    Map<String, Object> all(EmailConfigFilter filter);

    ApiResponse<?> add(EmailConfigAddRequest request);

    void delete(String id);

    void testConnection(String id);
}
