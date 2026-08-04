package com.frezo.email.config;

import com.frezo.email.entity.EmailTemplate;
import com.frezo.email.repository.EmailTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seed template email cho OTP / thông báo urgent — idempotent (WHERE NOT EXISTS by code).
 */
@Slf4j
@Component
@Order(40)
@RequiredArgsConstructor
public class EmailOtpTemplateSeedRunner implements ApplicationRunner {

    private final EmailTemplateRepository emailTemplateRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed("URGENT_NOTIFICATION", "Thông báo khẩn", "{{title}}",
                "<div style=\"font-family:sans-serif;max-width:560px\">"
                        + "<h2 style=\"color:#059669\">{{title}}</h2>"
                        + "<p style=\"white-space:pre-line;line-height:1.6;color:#334155\">{{content}}</p>"
                        + "<p style=\"color:#94a3b8;font-size:12px\">Frezo ERP — không trả lời email này.</p></div>",
                "Fallback email cho 2FA / OTP / thông báo urgent.");

        seed("PASSWORD_RESET", "OTP khôi phục mật khẩu", "Mã OTP khôi phục mật khẩu Frezo",
                "<div style=\"font-family:sans-serif;max-width:560px\">"
                        + "<h2 style=\"color:#059669\">Khôi phục mật khẩu</h2>"
                        + "<p>Xin chào {{name}},</p>"
                        + "<p>Mã OTP của bạn là:</p>"
                        + "<p style=\"font-size:28px;font-weight:700;letter-spacing:6px;color:#0f172a\">{{otp}}</p>"
                        + "<p>Hiệu lực <strong>{{minutes}}</strong> phút. Không chia sẻ mã này.</p>"
                        + "<p style=\"color:#94a3b8;font-size:12px\">Nếu bạn không yêu cầu, hãy bỏ qua email này.</p></div>",
                "Gửi OTP khi quên mật khẩu.");
    }

    private void seed(String code, String name, String subject, String content, String description) {
        if (Boolean.TRUE.equals(emailTemplateRepository.existsByCode(code))) {
            return;
        }
        emailTemplateRepository.save(EmailTemplate.builder()
                .code(code)
                .name(name)
                .subject(subject)
                .content(content)
                .description(description)
                .build());
        log.info("Seeded email template {}", code);
    }
}
