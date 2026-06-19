package com.frezo.qlns.mapper;

import com.frezo.qlns.dto.request.TaxConfigRequest;
import com.frezo.qlns.dto.response.TaxConfigResponse;
import com.frezo.qlns.entity.TaxConfig;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaxConfigMapper {
    TaxConfigResponse toResponse(TaxConfig entity);
    TaxConfig toEntity(TaxConfigRequest request);
}
