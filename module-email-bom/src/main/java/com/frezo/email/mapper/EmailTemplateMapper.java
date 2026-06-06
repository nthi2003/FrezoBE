package com.frezo.email.mapper;

import com.frezo.email.dto.request.*;
import com.frezo.email.dto.response.EmailConfigResponse;
import com.frezo.email.dto.response.EmailTemplateResponse;
import com.frezo.email.entity.EmailConfig;
import com.frezo.email.entity.EmailTemplate;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;


@Mapper(componentModel = "spring" , unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EmailTemplateMapper {

    EmailTemplateResponse toResponse (EmailTemplate emailTemplate);
    List<EmailTemplateResponse> toResponseList (List<EmailTemplate> emailTemplates);
    EmailTemplate toEntity (EmailTemplateRequest request);
    void updateEntity(EmailTemplateRequest request, @MappingTarget EmailTemplate emailTemplate);

}
