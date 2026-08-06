package com.frezo.email.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.response.PageResponse;
import com.frezo.email.common.EmailErrorCode;
import com.frezo.customer.entity.Customer;
import com.frezo.customer.repository.CustomerRepository;
import com.frezo.email.dto.request.BulkEmailRequest;
import com.frezo.email.dto.request.SendEmailLogFilter;
import com.frezo.email.dto.response.BulkEmailResponse;
import com.frezo.email.dto.response.SendEmailLogResponse;
import com.frezo.email.entity.EmailConfig;
import com.frezo.email.entity.EmailTemplate;
import com.frezo.email.entity.SendEmail;
import com.frezo.email.repository.EmailConfigRepository;
import com.frezo.email.repository.EmailTemplateRepository;
import com.frezo.email.repository.SendEmailRepository;
import com.frezo.email.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private static final String TYPE_EMAIL = "EMAIL";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final int MAX_ERROR_LENGTH = 1000;

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
        } catch (MessagingException | MailException e) {
            log.error("Failed to send email to {} via {}:{} (config {}): {}",
                    to, mailSender.getHost(), mailSender.getPort(), config.getCode(), e.getMessage());
            throw new AppException(EmailErrorCode.SEND_FAILED, e);
        }
    }

    @Override
    public void sendByTemplate(String templateCode, Map<String, Object> params, List<String> recipients) {
        EmailTemplate template = emailTemplateRepository.findByCode(templateCode)
                .orElseThrow(() -> new AppException(EmailErrorCode.EMAIL_TEMPLATE_NOT_FOUND));

        EmailConfig config = resolveActiveConfig();

        String processedContent = processTemplate(template.getContent(), params);
        String processedSubject = processTemplate(template.getSubject(), params);

        List<String> sent = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        String firstError = null;
        for (String recipient : recipients) {
            try {
                sendEmail(config, recipient, processedSubject, processedContent);
                sent.add(recipient);
            } catch (Exception e) {
                log.error("Failed to send to {}: {}", recipient, e.getMessage());
                failed.add(recipient);
                if (firstError == null) firstError = describeFailure(config, e);
            }
        }

        // Log tách SUCCESS / FAILED để admin thấy được lần gửi thất bại trong lịch sử.
        if (!sent.isEmpty()) {
            logSendEmail(template.getId(), processedSubject, sent, params.toString(), STATUS_SUCCESS, null);
        }
        if (!failed.isEmpty()) {
            logSendEmail(template.getId(), processedSubject, failed, params.toString(), STATUS_FAILED, firstError);
        }
        // Không ai nhận được → báo lỗi để caller không tưởng đã gửi.
        if (sent.isEmpty()) {
            throw new AppException(EmailErrorCode.SEND_FAILED);
        }
    }

    @Override
    public void sendSimple(String to, String subject, String htmlBody) {
        EmailConfig config = resolveActiveConfig();
        try {
            sendEmail(config, to, subject, htmlBody);
        } catch (Exception e) {
            logSendEmail(null, subject, List.of(to), "sendSimple", STATUS_FAILED, describeFailure(config, e));
            throw e;
        }
        logSendEmail(null, subject, List.of(to), "sendSimple", STATUS_SUCCESS, null);
    }

    /**
     * Config SMTP đang dùng. Nhiều row {@code activated} thì lấy row cập nhật gần nhất
     * (thứ tự của {@code findByActivatedTrue} không xác định — từng chọn nhầm MailHog).
     */
    private EmailConfig resolveActiveConfig() {
        return emailConfigRepository.findByActivatedTrue().stream()
                .filter(c -> !Boolean.TRUE.equals(c.getIsDeleted()))
                .max(Comparator.comparing(EmailConfig::getUpdatedDate,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElseThrow(() -> new AppException(EmailErrorCode.CONFIG_NOT_FOUND));
    }

    @Override
    public BulkEmailResponse sendBulk(BulkEmailRequest request) {
        EmailConfig config = resolveActiveConfig();

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
                    .orElseThrow(() -> new AppException(EmailErrorCode.EMAIL_TEMPLATE_NOT_FOUND));
        }

        if (recipients.isEmpty() && customers.isEmpty()) {
            throw new AppException(EmailErrorCode.NO_RECIPIENTS);
        }

        long success = 0;
        List<String> failedEmails = new ArrayList<>();
        List<String> sentEmails = new ArrayList<>();
        String firstError = null;

        // Gửi đến danh sách email trực tiếp (không personalize)
        for (String recipient : recipients) {
            try {
                String subj = request.getSubject() != null ? request.getSubject()
                        : (template != null ? template.getSubject() : "");
                String body = request.getBody() != null ? request.getBody()
                        : (template != null ? template.getContent() : "");
                sendEmail(config, recipient, subj, body);
                success++;
                sentEmails.add(recipient);
            } catch (Exception e) {
                log.error("Failed to send to {}: {}", recipient, e.getMessage());
                failedEmails.add(recipient);
                if (firstError == null) firstError = describeFailure(config, e);
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
                sentEmails.add(customer.getEmail());
            } catch (Exception e) {
                log.error("Failed to send to {}: {}", customer.getEmail(), e.getMessage());
                failedEmails.add(customer.getEmail());
                if (firstError == null) firstError = describeFailure(config, e);
            }
        }

        String templateId = template != null ? template.getId() : null;
        if (!sentEmails.isEmpty()) {
            logSendEmail(templateId, request.getSubject(), sentEmails,
                    request.getDescription(), STATUS_SUCCESS, null);
        }
        if (!failedEmails.isEmpty()) {
            logSendEmail(templateId, request.getSubject(), failedEmails,
                    request.getDescription(), STATUS_FAILED, firstError);
        }

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
    @Transactional(readOnly = true)
    public PageResponse<SendEmailLogResponse> getSendLogs(SendEmailLogFilter filter) {
        Pageable pageable = ServiceHelper.createPageable(filter.getPageNumber(), filter.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdDate"));
        Page<SendEmail> page = sendEmailRepository.findAll(buildSendLogSpec(filter), pageable);
        return PageResponse.from(page, EmailServiceImpl::toSendLogResponse);
    }

    private static Specification<SendEmail> buildSendLogSpec(SendEmailLogFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.or(cb.isFalse(root.get("isDeleted")), cb.isNull(root.get("isDeleted"))));

            if (StringUtils.hasText(filter.getStatus())) {
                predicates.add(cb.equal(cb.upper(root.get("status")), filter.getStatus().trim().toUpperCase()));
            }
            if (StringUtils.hasText(filter.getType())) {
                predicates.add(cb.equal(cb.upper(root.get("type")), filter.getType().trim().toUpperCase()));
            }
            if (filter.getFromDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), filter.getFromDate()));
            }
            if (filter.getToDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"), filter.getToDate()));
            }
            if (StringUtils.hasText(filter.getKeyword())) {
                String like = "%" + filter.getKeyword().trim().toLowerCase() + "%";
                // Người nhận nằm ở bảng con → join, và distinct để không nhân dòng.
                Join<Object, Object> recipientJoin = root.join("recipients", JoinType.LEFT);
                if (query != null) query.distinct(true);
                predicates.add(cb.or(
                        cb.like(cb.lower(cb.coalesce(root.get("topic"), "")), like),
                        cb.like(cb.lower(cb.coalesce(root.get("errorMessage"), "")), like),
                        cb.like(cb.lower(recipientJoin.as(String.class)), like)));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static SendEmailLogResponse toSendLogResponse(SendEmail entity) {
        return SendEmailLogResponse.builder()
                .id(entity.getId())
                .topic(entity.getTopic())
                .recipients(entity.getRecipients() == null ? List.of() : List.copyOf(entity.getRecipients()))
                .type(entity.getType() != null ? entity.getType() : TYPE_EMAIL)
                .status(entity.getStatus() != null ? entity.getStatus() : STATUS_SUCCESS)
                .errorMessage(entity.getErrorMessage())
                .description(entity.getDescription())
                .emailTemplateId(entity.getEmailTemplateId())
                .createdDate(entity.getCreatedDate())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    @Override
    public void testConnection(String configId) {
        EmailConfig config = emailConfigRepository.findById(configId)
                .orElseThrow(() -> new AppException(EmailErrorCode.CONFIG_NOT_FOUND));

        JavaMailSenderImpl mailSender = createMailSender(config);
        try {
            mailSender.testConnection();
            log.info("Email connection test successful for config: {}", config.getName());
        } catch (MessagingException e) {
            log.error("Email connection test failed for config {}: {}", config.getName(), e.getMessage());
            throw new AppException(EmailErrorCode.CONNECTION_FAILED);
        }
    }

    private void logSendEmail(String templateId, String topic, List<String> recipients,
                              String description, String status, String errorMessage) {
        try {
            SendEmail logEntry = SendEmail.builder()
                    .emailTemplateId(templateId)
                    .topic(topic)
                    .recipients(recipients)
                    .description(description)
                    .type(TYPE_EMAIL)
                    .status(status)
                    .errorMessage(truncate(errorMessage, MAX_ERROR_LENGTH))
                    .build();
            sendEmailRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to log sent email: {}", e.getMessage());
        }
    }

    /** Lý do thất bại đủ cụ thể để admin sửa được cấu hình (host/port + root cause). */
    private static String describeFailure(EmailConfig config, Throwable e) {
        Throwable root = e;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        String target = config == null ? "?" : config.getSmtp() + ":" + config.getPort();
        return target + " — " + root.getClass().getSimpleName() + ": " + root.getMessage();
    }

    private static String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private JavaMailSenderImpl createMailSender(EmailConfig config) {
        String host = config.getSmtp() == null ? "" : config.getSmtp().trim();
        // Nhập email vào ô SMTP là lỗi cấu hình hay gặp → UnknownHostException khó hiểu.
        if (host.isEmpty() || host.contains("@")) {
            log.error("Email config {} có SMTP host không hợp lệ: '{}' (cần dạng smtp.gmail.com)",
                    config.getCode(), host);
            throw new AppException(EmailErrorCode.CONFIG_NOT_FOUND);
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(config.getPort());
        mailSender.setUsername(config.getNameEmail() == null ? null : config.getNameEmail().trim());
        // App password Gmail thường được dán kèm khoảng trắng → auth fail.
        mailSender.setPassword(config.getApiKey() == null ? null : config.getApiKey().replace(" ", ""));

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");
        props.put("mail.smtp.writetimeout", "10000");
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
