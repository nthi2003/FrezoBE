package com.frezo.qtht.mapper;

import com.frezo.qtht.dto.response.OrganizationDetailResponse;
import com.frezo.qtht.entity.Organization;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrganizationDetailMapper {
    OrganizationDetailResponse toResponse(Organization organization);
}
