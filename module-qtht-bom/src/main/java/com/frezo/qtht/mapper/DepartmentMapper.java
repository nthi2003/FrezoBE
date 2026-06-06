package com.frezo.qtht.mapper;

import com.frezo.qtht.dto.request.DepartmentSaveRequest;
import com.frezo.qtht.dto.response.DepartmentResponse;
import com.frezo.qtht.entity.Department;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface DepartmentMapper {

    @Mapping(target = "organizationName", source = "organization.name")
    @Mapping(target = "parentName", source = "parent.name")
    @Mapping(target = "managerName", source = "manager.name")
    @Mapping(target = "deputyManagerName", source = "deputyManager.name")
    DepartmentResponse toResponse(Department department);

    List<DepartmentResponse> toResponseList(List<Department> department);

    Department toEntity(DepartmentSaveRequest request);


    void updateEntity(DepartmentSaveRequest request, @MappingTarget Department department);
}
