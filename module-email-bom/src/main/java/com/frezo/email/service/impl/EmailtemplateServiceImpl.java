package com.frezo.email.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.email.common.EmailErrorCode;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.email.dto.request.EmailTemplateFilter;
import com.frezo.email.dto.request.EmailTemplateRequest;
import com.frezo.email.dto.response.EmailTemplateResponse;
import com.frezo.email.entity.EmailTemplate;
import com.frezo.email.mapper.EmailTemplateMapper;
import com.frezo.email.repository.EmailTemplateRepository;
import com.frezo.email.service.EmailtemplateService;
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
public class EmailtemplateServiceImpl implements EmailtemplateService {
    private final EmailTemplateMapper emailTemplateMapper;
    private final EmailTemplateRepository emailTemplateRepository;


    public PageResponse<EmailTemplateResponse> all(EmailTemplateFilter filter) {
        Specification<EmailTemplate> specification = createSpecification(filter);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate" );
        Page<EmailTemplate> entities = emailTemplateRepository.findAll(specification, ServiceHelper.createPageable(filter.getPageNumber() , filter.getPageSize(), sort));
        return PageResponse.from(entities, emailTemplateMapper::toResponse);
    }

    @Transactional
    public ApiResponse<?> add(EmailTemplateRequest request) {

        validateRequest(request);
        EmailTemplate emailTemplate = emailTemplateMapper.toEntity(request);
        emailTemplate.setIsDeleted(false);
        EmailTemplate saveEmail = emailTemplateRepository.save(emailTemplate);
        return ApiResponse.ok(emailTemplateMapper.toResponse(saveEmail));

    }
    @Transactional
    public ApiResponse<?> edit(String id, EmailTemplateRequest request) {
        EmailTemplate exist = findEntityById(id);
        validateRequest(request);
        emailTemplateMapper.updateEntity(request, exist);
        EmailTemplate saveEmail = emailTemplateRepository.save(exist);
        return ApiResponse.ok(emailTemplateMapper.toResponse(saveEmail));
    }

    public EmailTemplateResponse view (String id) {
       EmailTemplate emailTemplate = findEntityById(id);
       return emailTemplateMapper.toResponse(emailTemplate);
    }

    @Transactional(readOnly = true)
    protected EmailTemplate findEntityById(String id) {
        return emailTemplateRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(EmailErrorCode.TEMPLATE_NOT_FOUND));
    }

    private void validateRequest(EmailTemplateRequest request) {
        if (request.getCode() != null) {
            if (emailTemplateRepository.existsByCode(request.getCode())) {
                throw new AppException(EmailErrorCode.TEMPLATE_CODE_EXISTS);
            } else if (emailTemplateRepository.existsByName(request.getName())) {
                throw new AppException(EmailErrorCode.TEMPLATE_NAME_EXISTS);
            }
        }
    }
    public void delete (String id) {
       EmailTemplate emailTemplate  = findEntityById(id);
       emailTemplate.setIsDeleted(true);
       emailTemplateRepository.save(emailTemplate);

    }
    private Specification<EmailTemplate> createSpecification (EmailTemplateFilter filter) {
        Specification<EmailTemplate> specification = Specification
            .where(GenericSpecification.hasFieldIs("isDeleted" , Boolean.FALSE));
        if (SystemUtils.isNotNullOrEmpty(filter.getKeyword())) {
            specification = specification.and(
                GenericSpecification.<EmailTemplate>likeField("name", filter.getKeyword())
                    .or( GenericSpecification.likeField("subject", filter.getKeyword()))
                    .or( GenericSpecification.likeField("description", filter.getKeyword()))
                    .or( GenericSpecification.likeField("code", filter.getKeyword()))
            );
        }
        return specification;
    }

}
