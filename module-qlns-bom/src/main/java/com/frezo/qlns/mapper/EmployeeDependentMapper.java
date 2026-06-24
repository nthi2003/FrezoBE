package com.frezo.qlns.mapper;

import com.frezo.qlns.dto.request.EmployeeDependentRequest;
import com.frezo.qlns.dto.response.EmployeeDependentResponse;
import com.frezo.qlns.entity.EmployeeDependent;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EmployeeDependentMapper {
    EmployeeDependentResponse toResponse(EmployeeDependent entity);
    EmployeeDependent toEntity(EmployeeDependentRequest request);
}
