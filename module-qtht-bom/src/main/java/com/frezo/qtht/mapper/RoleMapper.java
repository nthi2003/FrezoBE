package com.frezo.qtht.mapper;

import com.frezo.qtht.dto.response.RoleResponse;
import com.frezo.qtht.entity.Role;
import org.mapstruct.*;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface RoleMapper {

    RoleResponse toResponse(Role role);

    List<RoleResponse> toResponseList(List<Role> roles);
}