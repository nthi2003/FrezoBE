package com.frezo.email.service;

import com.frezo.email.dto.request.BulkEmailRequest;
import com.frezo.email.dto.response.BulkEmailResponse;
import com.frezo.email.entity.EmailConfig;

import java.util.List;
import java.util.Map;

public interface EmailService {

    void sendEmail(EmailConfig config, String to, String subject, String body);

    void sendByTemplate(String templateCode, Map<String, Object> params, List<String> recipients);

    BulkEmailResponse sendBulk(BulkEmailRequest request);

    BulkEmailResponse sendBulkByCategoryCodes(String templateCode, String subject, String body, List<String> categoryCodes, String description);

    void testConnection(String configId);
}
