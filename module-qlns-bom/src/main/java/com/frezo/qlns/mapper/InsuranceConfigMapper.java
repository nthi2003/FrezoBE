package com.frezo.qlns.mapper;

import com.frezo.qlns.dto.request.InsuranceConfigRequest;
import com.frezo.qlns.dto.response.InsuranceConfigResponse;
import com.frezo.qlns.entity.InsuranceConfig;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InsuranceConfigMapper {
    InsuranceConfigResponse toResponse(InsuranceConfig entity);
    InsuranceConfig toEntity(InsuranceConfigRequest request);
}
