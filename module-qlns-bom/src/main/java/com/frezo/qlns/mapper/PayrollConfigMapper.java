package com.frezo.qlns.mapper;

import com.frezo.qlns.dto.request.PayrollConfigRequest;
import com.frezo.qlns.dto.response.PayrollConfigResponse;
import com.frezo.qlns.entity.PayrollConfig;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PayrollConfigMapper {
    PayrollConfigResponse toResponse(PayrollConfig entity);
    PayrollConfig toEntity(PayrollConfigRequest request);
}
