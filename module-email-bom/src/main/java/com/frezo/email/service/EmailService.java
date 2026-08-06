package com.frezo.email.service;

import com.frezo.common.response.PageResponse;
import com.frezo.email.dto.request.BulkEmailRequest;
import com.frezo.email.dto.request.SendEmailLogFilter;
import com.frezo.email.dto.response.BulkEmailResponse;
import com.frezo.email.dto.response.SendEmailLogResponse;
import com.frezo.email.entity.EmailConfig;

import java.util.List;
import java.util.Map;

public interface EmailService {

    void sendEmail(EmailConfig config, String to, String subject, String body);

    void sendByTemplate(String templateCode, Map<String, Object> params, List<String> recipients);

    void sendSimple(String to, String subject, String htmlBody);

    BulkEmailResponse sendBulk(BulkEmailRequest request);

    BulkEmailResponse sendBulkByCategoryCodes(String templateCode, String subject, String body, List<String> categoryCodes, String description);

    void testConnection(String configId);

    /** Lịch sử gửi email (SUCCESS + FAILED) cho admin theo dõi. */
    PageResponse<SendEmailLogResponse> getSendLogs(SendEmailLogFilter filter);
}
