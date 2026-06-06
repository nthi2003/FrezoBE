package com.frezo.email.service;

import com.frezo.email.entity.EmailConfig;
import java.util.List;
import java.util.Map;

public interface EmailService {
    /**
     * Gửi email cơ bản
     */
    void sendEmail(EmailConfig config, String to, String subject, String body);

    /**
     * Gửi email sử dụng template và tham số placeholder
     * Ví dụ: template có {{name}}, params có {"name": "Test"}
     */
    void sendByTemplate(String templateCode, Map<String, Object> params, List<String> recipients);

    /**
     * Kiểm tra cấu hình mail có hoạt động không
     */
    void testConnection(String configId);
}
