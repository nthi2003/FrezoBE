package com.frezo.qlns.mapper;

import com.frezo.qlns.dto.response.PayrollResponse;
import com.frezo.qlns.entity.Payroll;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PayrollMapper {
    
    @Mapping(target = "baseSalary", source = "basicSalary")
    @Mapping(target = "payMonth", source = "month")
    @Mapping(target = "payYear", source = "year")
    @Mapping(target = "statusLabel", expression = "java(getStatusLabel(entity.getStatus()))")
    PayrollResponse toResponse(Payroll entity);

    default String getStatusLabel(Integer status) {
        if (status == null) return "Bản nháp";
        return switch (status) {
            case 0 -> "Bản nháp";
            case 1 -> "Đã xác nhận";
            case 2 -> "Đã thanh toán";
            default -> "Không xác định";
        };
    }
}
