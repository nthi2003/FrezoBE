package com.frezo.fbautomation.mapper;

import com.frezo.fbautomation.dto.request.FacebookAccountRequest;
import com.frezo.fbautomation.dto.response.FacebookAccountResponse;
import com.frezo.fbautomation.entity.FacebookAccount;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FacebookAccountMapper {

    FacebookAccountResponse toResponse(FacebookAccount account);

    @Mapping(target = "postsToday", ignore = true)
    FacebookAccount toEntity(FacebookAccountRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget FacebookAccount entity, FacebookAccountRequest request);
}
