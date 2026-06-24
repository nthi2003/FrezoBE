package com.frezo.email.service.impl;

import com.frezo.common.exception.QTHTException;
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
import com.frezo.util.web.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailConfigServiceImpl implements EmailConfigService {
    private final EmailConfigRepository emailConfigRepository;
    private final EmailConfigMapper emailConfigMapper;
    private final EmailService emailService;

    public Map<String, Object> all(EmailConfigFilter filter) {
        Specification<EmailConfig> specification = createSpecification(filter);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate");
        Page<EmailConfig> entities = emailConfigRepository.findAll(specification,
                ServiceHelper.createPageable(filter.getPageNumber(), filter.getPageSize(), sort));
        List<EmailConfigResponse> responses = entities.getContent().stream()
                .map(emailConfigMapper::toResponse).toList();
        return ServiceHelper.createResponse1(filter.getPageNumber(), filter.getPageSize(), entities, responses);
    }

    public Response<?> add(EmailConfigAddRequest request) {
        validateRequest(request);
        EmailConfig emailConfig = emailConfigMapper.toEntity(request);
        emailConfig.setIsDeleted(false);
        EmailConfig saveEmail = emailConfigRepository.save(emailConfig);
        return Response.ok(emailConfigMapper.toResponse(saveEmail));

    }

    public Response<?> edit(String id, EmailConfigEditRequest request) {
        EmailConfig exist = findEntityById(id);
        validateRequestEdit(id, request);
        emailConfigMapper.updateEntity(request, exist);
        EmailConfig saveEmail = emailConfigRepository.save(exist);
        return Response.ok(emailConfigMapper.toResponse(saveEmail));
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
            throw new QTHTException("valid.code.exists");
        }
        if (emailConfigRepository.existsByName(request.getName())) {
            throw new QTHTException("valid.name.exists");
        }
        if (emailConfigRepository.existsBySmtp(request.getSmtp())) {
            throw new QTHTException("valid.smtp.exists");
        }
        if (emailConfigRepository.existsByNameEmail(request.getNameEmail())) {
            throw new QTHTException("valid.nameEmail.exists");
        }
    }

    private void validateRequestEdit(String id, EmailConfigEditRequest request) {
        if (emailConfigRepository.existsByCodeAndIdNot(request.getCode(), id)) {
            throw new QTHTException("valid.code.exists");
        }
        if (emailConfigRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new QTHTException("valid.name.exists");
        }
        if (emailConfigRepository.existsBySmtpAndIdNot(request.getSmtp(), id)) {
            throw new QTHTException("valid.smtp.exists");
        }
        if (emailConfigRepository.existsByNameEmailAndIdNot(request.getNameEmail(), id)) {
            throw new QTHTException("valid.nameEmail.exists");
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

        emailConfig.setActivated(true);
        emailConfigRepository.save(emailConfig);
    }

    protected EmailConfig findEntityById(String id) {

        return emailConfigRepository.findById(id).orElseThrow(() -> new QTHTException("valid.not.found"));
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
