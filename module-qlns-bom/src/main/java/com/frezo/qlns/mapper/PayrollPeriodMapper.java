package com.frezo.qlns.mapper;

import com.frezo.qlns.dto.response.PayrollPeriodResponse;
import com.frezo.qlns.entity.PayrollPeriod;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PayrollPeriodMapper {

    @Mapping(target = "statusLabel", expression = "java(getStatusLabel(entity.getStatus()))")
    PayrollPeriodResponse toResponse(PayrollPeriod entity);

    default String getStatusLabel(Integer status) {
        if (status == null) return "Mở";
        return switch (status) {
            case 0 -> "Mở";
            case 1 -> "Đã khóa";
            case 2 -> "Đã đóng";
            default -> "Không xác định";
        };
    }
}
