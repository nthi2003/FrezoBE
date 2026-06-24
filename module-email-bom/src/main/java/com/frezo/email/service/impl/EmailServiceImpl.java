package com.frezo.email.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.customer.entity.Customer;
import com.frezo.customer.repository.CustomerRepository;
import com.frezo.email.dto.request.BulkEmailRequest;
import com.frezo.email.dto.response.BulkEmailResponse;
import com.frezo.email.entity.EmailConfig;
import com.frezo.email.entity.EmailTemplate;
import com.frezo.email.entity.SendEmail;
import com.frezo.email.repository.EmailConfigRepository;
import com.frezo.email.repository.EmailTemplateRepository;
import com.frezo.email.repository.SendEmailRepository;
import com.frezo.email.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final EmailConfigRepository emailConfigRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final SendEmailRepository sendEmailRepository;
    private final CustomerRepository customerRepository;

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
            try {
                sendEmail(config, recipient, processedSubject, processedContent);
            } catch (Exception e) {
                log.error("Failed to send to {}: {}", recipient, e.getMessage());
            }
        }

        logSendEmail(template.getId(), processedSubject, recipients, params.toString());
    }

    @Override
    public BulkEmailResponse sendBulk(BulkEmailRequest request) {
        EmailConfig config = emailConfigRepository.findByActivatedTrue()
                .stream().findFirst()
                .orElseThrow(() -> new QTHTException("error.email.config.not.found"));

        List<String> recipients = request.getRecipients() != null ? request.getRecipients() : new ArrayList<>();
        List<Customer> customers = new ArrayList<>();

        if (request.getCategoryCodes() != null && !request.getCategoryCodes().isEmpty()) {
            customers = customerRepository.findByCategoryCodeInAndEmailIsNotNull(request.getCategoryCodes());
        }

        boolean useTemplate = request.getTemplateCode() != null
                && (request.getSubject() == null || request.getBody() == null);

        EmailTemplate template = null;
        if (useTemplate) {
            template = emailTemplateRepository.findByCode(request.getTemplateCode())
                    .orElseThrow(() -> new QTHTException("error.email.template.not.found"));
        }

        if (recipients.isEmpty() && customers.isEmpty()) {
            throw new QTHTException("error.email.no.recipients");
        }

        long success = 0;
        List<String> failedEmails = new ArrayList<>();

        // Gửi đến danh sách email trực tiếp (không personalize)
        for (String recipient : recipients) {
            try {
                String subj = request.getSubject() != null ? request.getSubject()
                        : (template != null ? template.getSubject() : "");
                String body = request.getBody() != null ? request.getBody()
                        : (template != null ? template.getContent() : "");
                sendEmail(config, recipient, subj, body);
                success++;
            } catch (Exception e) {
                log.error("Failed to send to {}: {}", recipient, e.getMessage());
                failedEmails.add(recipient);
            }
        }

        // Gửi đến khách hàng theo nhóm (personalize: thay {{name}}, {{email}},... tự động)
        for (Customer customer : customers) {
            if (customer.getEmail() == null || customer.getEmail().isBlank()) continue;
            try {
                Map<String, Object> params = Map.of(
                        "name", customer.getName() != null ? customer.getName() : "",
                        "email", customer.getEmail(),
                        "code", customer.getCode() != null ? customer.getCode() : "",
                        "phone", customer.getPhone() != null ? customer.getPhone() : "",
                        "address", customer.getAddress() != null ? customer.getAddress() : "",
                        "taxCode", customer.getTaxCode() != null ? customer.getTaxCode() : "",
                        "type", customer.getType() != null ? customer.getType() : "",
                        "status", customer.getStatus() != null ? customer.getStatus() : ""
                );

                String subj = request.getSubject() != null
                        ? processTemplate(request.getSubject(), params)
                        : (template != null ? processTemplate(template.getSubject(), params) : "");
                String body = request.getBody() != null
                        ? processTemplate(request.getBody(), params)
                        : (template != null ? processTemplate(template.getContent(), params) : "");

                sendEmail(config, customer.getEmail(), subj, body);
                success++;
            } catch (Exception e) {
                log.error("Failed to send to {}: {}", customer.getEmail(), e.getMessage());
                failedEmails.add(customer.getEmail());
            }
        }

        logSendEmail(template != null ? template.getId() : null,
                request.getSubject(), customers.stream().map(Customer::getEmail).toList(),
                request.getDescription());

        return BulkEmailResponse.builder()
                .totalRecipients(recipients.size() + customers.size())
                .sentSuccess(success)
                .sentFailed(failedEmails.size())
                .failedEmails(failedEmails)
                .build();
    }

    @Override
    public BulkEmailResponse sendBulkByCategoryCodes(String templateCode, String subject, String body, List<String> categoryCodes, String description) {
        BulkEmailRequest request = BulkEmailRequest.builder()
                .templateCode(templateCode)
                .subject(subject)
                .body(body)
                .categoryCodes(categoryCodes)
                .description(description)
                .build();
        return sendBulk(request);
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

    private void logSendEmail(String templateId, String topic, List<String> recipients, String description) {
        try {
            SendEmail logEntry = SendEmail.builder()
                    .emailTemplateId(templateId)
                    .topic(topic)
                    .recipients(recipients)
                    .description(description)
                    .build();
            sendEmailRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to log sent email: {}", e.getMessage());
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
        props.put("mail.smtp.starttls.enable", "true");
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
