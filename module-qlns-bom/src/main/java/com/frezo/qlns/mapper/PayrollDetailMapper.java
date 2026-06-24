package com.frezo.qlns.mapper;

import com.frezo.qlns.dto.response.PayrollDetailResponse;
import com.frezo.qlns.entity.PayrollDetail;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PayrollDetailMapper {
    PayrollDetailResponse toResponse(PayrollDetail entity);
}
