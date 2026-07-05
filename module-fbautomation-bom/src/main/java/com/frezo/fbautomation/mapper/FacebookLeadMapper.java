package com.frezo.fbautomation.mapper;

import com.frezo.fbautomation.dto.request.FacebookLeadRequest;
import com.frezo.fbautomation.dto.response.FacebookLeadResponse;
import com.frezo.fbautomation.entity.FacebookLead;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FacebookLeadMapper {

    FacebookLeadResponse toResponse(FacebookLead lead);

    @Mapping(target = "importedCustomerId", ignore = true)
    FacebookLead toEntity(FacebookLeadRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget FacebookLead entity, FacebookLeadRequest request);
}
