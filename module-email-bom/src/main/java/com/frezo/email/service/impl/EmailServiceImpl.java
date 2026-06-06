package com.frezo.email.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.email.entity.EmailConfig;
import com.frezo.email.entity.EmailTemplate;
import com.frezo.email.repository.EmailConfigRepository;
import com.frezo.email.repository.EmailTemplateRepository;
import com.frezo.email.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final EmailConfigRepository emailConfigRepository;
    private final EmailTemplateRepository emailTemplateRepository;

    @Override
    public void sendEmail(EmailConfig config, String to, String subject, String body) {
        JavaMailSenderImpl mailSender = createMailSender(config);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(config.getNameEmail());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(message);
            log.info("Email sent successfully to {}", to);
        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            throw new QTHTException("error.email.send.failed");
        }
    }

    @Override
    public void sendByTemplate(String templateCode, Map<String, Object> params, List<String> recipients) {
        EmailTemplate template = emailTemplateRepository.findByCode(templateCode)
                .orElseThrow(() -> new QTHTException("error.email.template.not.found"));

        EmailConfig config = emailConfigRepository.findByActivatedTrue()
                .stream().findFirst()
                .orElseThrow(() -> new QTHTException("error.email.config.not.found"));

        String processedContent = processTemplate(template.getContent(), params);
        String processedSubject = processTemplate(template.getSubject(), params);

        for (String recipient : recipients) {
            sendEmail(config, recipient, processedSubject, processedContent);
        }
    }

    @Override
    public void testConnection(String configId) {
        EmailConfig config = emailConfigRepository.findById(configId)
                .orElseThrow(() -> new QTHTException("error.email.config.not.found"));

        JavaMailSenderImpl mailSender = createMailSender(config);
        try {
            mailSender.testConnection();
            log.info("Email connection test successful for config: {}", config.getName());
        } catch (MessagingException e) {
            log.error("Email connection test failed for config {}: {}", config.getName(), e.getMessage());
            throw new QTHTException("error.email.connection.failed");
        }
    }

    private JavaMailSenderImpl createMailSender(EmailConfig config) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(config.getSmtp());
        mailSender.setPort(config.getPort());
        mailSender.setUsername(config.getNameEmail());
        mailSender.setPassword(config.getApiKey());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true"); // Giả định dùng STARTTLS
        props.put("mail.debug", "false");

        return mailSender;
    }

    private String processTemplate(String content, Map<String, Object> params) {
        if (content == null)
            return "";
        String result = content;
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", String.valueOf(entry.getValue()));
        }
        return result;
    }
}
