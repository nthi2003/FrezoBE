package com.frezo.email.mapper;

import com.frezo.email.dto.request.EmailGroupRequest;
import com.frezo.email.dto.response.EmailGroupResponse;
import com.frezo.email.entity.EmailGroup;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EmailGroupMapper {

    EmailGroupResponse toResponse(EmailGroup emailGroup);
    List<EmailGroupResponse> toResponseList(List<EmailGroup> emailGroups);
    EmailGroup toEntity(EmailGroupRequest request);
    void updateEntity(EmailGroupRequest request, @MappingTarget EmailGroup emailGroup);
}
