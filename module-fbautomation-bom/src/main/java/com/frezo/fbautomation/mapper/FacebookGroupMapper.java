package com.frezo.fbautomation.mapper;

import com.frezo.fbautomation.dto.request.FacebookGroupRequest;
import com.frezo.fbautomation.dto.response.FacebookGroupResponse;
import com.frezo.fbautomation.entity.FacebookGroup;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FacebookGroupMapper {

    FacebookGroupResponse toResponse(FacebookGroup group);

    FacebookGroup toEntity(FacebookGroupRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget FacebookGroup entity, FacebookGroupRequest request);
}
