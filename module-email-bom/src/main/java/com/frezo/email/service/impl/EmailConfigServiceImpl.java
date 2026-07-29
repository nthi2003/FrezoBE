package com.frezo.email.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.email.common.EmailErrorCode;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.email.dto.request.EmailConfigAddRequest;
import com.frezo.email.dto.request.EmailConfigEditRequest;
import com.frezo.email.dto.request.EmailConfigFilter;
import com.frezo.email.dto.response.EmailConfigResponse;
import com.frezo.email.entity.EmailConfig;
import com.frezo.email.mapper.EmailConfigMapper;
import com.frezo.email.repository.EmailConfigRepository;
import com.frezo.email.service.EmailConfigService;
import com.frezo.email.service.EmailService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailConfigServiceImpl implements EmailConfigService {
    private final EmailConfigRepository emailConfigRepository;
    private final EmailConfigMapper emailConfigMapper;
    private final EmailService emailService;

    public PageResponse<EmailConfigResponse> all(EmailConfigFilter filter) {
        Specification<EmailConfig> specification = createSpecification(filter);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate");
        Page<EmailConfig> entities = emailConfigRepository.findAll(specification,
                ServiceHelper.createPageable(filter.getPageNumber(), filter.getPageSize(), sort));
        return PageResponse.from(entities, emailConfigMapper::toResponse);
    }

    public ApiResponse<?> add(EmailConfigAddRequest request) {
        validateRequest(request);
        EmailConfig emailConfig = emailConfigMapper.toEntity(request);
        emailConfig.setIsDeleted(false);
        // Bulk/send dùng findByActivatedTrue() — tạo mới mặc định chưa active;
        // user phải gọi PUT /email/config/{id}/activate (hoặc nút Activate trên UI).
        if (emailConfig.getActivated() == null) {
            emailConfig.setActivated(false);
        }
        EmailConfig saveEmail = emailConfigRepository.save(emailConfig);
        return ApiResponse.ok(emailConfigMapper.toResponse(saveEmail));

    }

    public ApiResponse<?> edit(String id, EmailConfigEditRequest request) {
        EmailConfig exist = findEntityById(id);
        validateRequestEdit(id, request);
        emailConfigMapper.updateEntity(request, exist);
        EmailConfig saveEmail = emailConfigRepository.save(exist);
        return ApiResponse.ok(emailConfigMapper.toResponse(saveEmail));
    }

    private Specification<EmailConfig> createSpecification(EmailConfigFilter filter) {
        Specification<EmailConfig> specification = Specification
                .where(GenericSpecification.hasFieldIs("isDeleted", Boolean.FALSE));
        if (SystemUtils.isNotNullOrEmpty(filter.getKeyword())) {
            specification = specification.and(
                    GenericSpecification.<EmailConfig>likeField("name", filter.getKeyword())
                            .or(GenericSpecification.likeField("apiKey", filter.getKeyword()))
                            .or(GenericSpecification.likeField("smtp", filter.getKeyword())));

        }
        return specification;
    }

    private void validateRequest(EmailConfigAddRequest request) {
        if (emailConfigRepository.existsByCode(request.getCode())) {
            throw new AppException(EmailErrorCode.CODE_EXISTS);
        }
        if (emailConfigRepository.existsByName(request.getName())) {
            throw new AppException(EmailErrorCode.NAME_EXISTS);
        }
        if (emailConfigRepository.existsBySmtp(request.getSmtp())) {
            throw new AppException(EmailErrorCode.SMTP_EXISTS);
        }
        if (emailConfigRepository.existsByNameEmail(request.getNameEmail())) {
            throw new AppException(EmailErrorCode.NAME_EMAIL_EXISTS);
        }
    }

    private void validateRequestEdit(String id, EmailConfigEditRequest request) {
        if (emailConfigRepository.existsByCodeAndIdNot(request.getCode(), id)) {
            throw new AppException(EmailErrorCode.CODE_EXISTS);
        }
        if (emailConfigRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new AppException(EmailErrorCode.NAME_EXISTS);
        }
        if (emailConfigRepository.existsBySmtpAndIdNot(request.getSmtp(), id)) {
            throw new AppException(EmailErrorCode.SMTP_EXISTS);
        }
        if (emailConfigRepository.existsByNameEmailAndIdNot(request.getNameEmail(), id)) {
            throw new AppException(EmailErrorCode.NAME_EMAIL_EXISTS);
        }
    }

    @Transactional
    public void deactivate(String id) {

        EmailConfig emailConfig = findEntityById(id);

        emailConfig.setActivated(false);
        emailConfigRepository.save(emailConfig);
    }

    @Transactional
    public void activate(String id) {
        EmailConfig emailConfig = findEntityById(id);
        // LNK-09: chỉ 1 config activated — deactivate các config khác trước
        for (EmailConfig other : emailConfigRepository.findByActivatedTrue()) {
            if (!id.equals(other.getId())) {
                other.setActivated(false);
                emailConfigRepository.save(other);
            }
        }
        emailConfig.setActivated(true);
        emailConfigRepository.save(emailConfig);
    }

    protected EmailConfig findEntityById(String id) {

        return emailConfigRepository.findById(id).orElseThrow(() -> new AppException(EmailErrorCode.CONFIG_ENTITY_NOT_FOUND));
    }

    @Transactional
    public void delete(String id) {
        EmailConfig emailConfig = findEntityById(id);
        emailConfig.setIsDeleted(true);
        emailConfigRepository.save(emailConfig);
    }

    @Override
    public void testConnection(String id) {
        emailService.testConnection(id);
    }

}
