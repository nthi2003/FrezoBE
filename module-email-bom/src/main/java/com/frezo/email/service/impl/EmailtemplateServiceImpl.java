package com.frezo.email.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.helper.GenericSpecification;
import com.frezo.common.helper.ServiceHelper;
import com.frezo.common.helper.SystemUtils;
import com.frezo.email.dto.request.EmailTemplateFilter;
import com.frezo.email.dto.request.EmailTemplateRequest;
import com.frezo.email.dto.response.EmailConfigResponse;
import com.frezo.email.dto.response.EmailTemplateResponse;
import com.frezo.email.entity.EmailTemplate;
import com.frezo.email.mapper.EmailTemplateMapper;
import com.frezo.email.repository.EmailTemplateRepository;
import com.frezo.email.service.EmailtemplateService;
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
public class EmailtemplateServiceImpl implements EmailtemplateService {
    private final EmailTemplateMapper emailTemplateMapper;
    private final EmailTemplateRepository emailTemplateRepository;


    public Map<String , Object> all(EmailTemplateFilter filter) {
        Specification<EmailTemplate> specification = createSpecification(filter);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdDate" );
        Page<EmailTemplate> entities = emailTemplateRepository.findAll(specification, ServiceHelper.createPageable(filter.getPageNumber() , filter.getPageSize(), sort));
        List<EmailTemplateResponse> responses = entities.getContent().stream()
            .map(emailTemplateMapper::toResponse).toList();
        return ServiceHelper.createResponse1(filter.getPageNumber(), filter.getPageSize(), entities , responses);
    }

    @Transactional
    public Response<?> add(EmailTemplateRequest request) {

        validateRequest(request);
        EmailTemplate emailTemplate = emailTemplateMapper.toEntity(request);
        emailTemplate.setIsDeleted(false);
        EmailTemplate saveEmail = emailTemplateRepository.save(emailTemplate);
        return Response.ok(emailTemplateMapper.toResponse(saveEmail));

    }
    @Transactional
    public Response<?> edit(String id, EmailTemplateRequest request) {
        EmailTemplate exist = findEntityById(id);
        validateRequest(request);
        emailTemplateMapper.updateEntity(request, exist);
        EmailTemplate saveEmail = emailTemplateRepository.save(exist);
        return Response.ok(emailTemplateMapper.toResponse(saveEmail));
    }

    public EmailTemplateResponse view (String id) {
       EmailTemplate emailTemplate = findEntityById(id);
       return emailTemplateMapper.toResponse(emailTemplate);
    }

    @Transactional(readOnly = true)
    protected EmailTemplate findEntityById(String id) {
        return emailTemplateRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new QTHTException("invalid.email.entity.not.found"));
    }

    private void validateRequest(EmailTemplateRequest request) {
        if (request.getCode() != null) {
            if (emailTemplateRepository.existsByCode(request.getCode())) {
                throw new QTHTException("validate.code.exist");
            } else if (emailTemplateRepository.existsByName(request.getName())) {
                throw new QTHTException("validate.name.exist");
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
