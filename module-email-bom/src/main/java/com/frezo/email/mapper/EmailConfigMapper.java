package com.frezo.email.mapper;

import com.frezo.email.dto.request.EmailConfigAddRequest;
import com.frezo.email.dto.request.EmailConfigEditRequest;
import com.frezo.email.dto.response.EmailConfigResponse;
import com.frezo.email.entity.EmailConfig;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring" , unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EmailConfigMapper {

    EmailConfigResponse toResponse (EmailConfig emailConfig);
    List<EmailConfigResponse> toResponseList (List<EmailConfig> emailConfigs);
    EmailConfig toEntity (EmailConfigAddRequest request);
    void updateEntity(EmailConfigEditRequest request, @MappingTarget EmailConfig emailConfig);


}
