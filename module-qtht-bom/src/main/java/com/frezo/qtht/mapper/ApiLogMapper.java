package com.frezo.qtht.mapper;

import com.frezo.qtht.dto.response.ApiLogResponse;
import com.frezo.qtht.entity.ApiLog;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ApiLogMapper {
    @Mapping(source = "requestBody", target = "requestBody")
    @Mapping(source = "responseBody", target = "responseBody")
    ApiLogResponse toResponse(ApiLog apiLog);
}
