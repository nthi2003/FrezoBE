package com.frezo.qtht.mapper;

import com.frezo.qtht.dto.request.RoleMenuAssignRequest;
import com.frezo.qtht.dto.response.RoleMenuResponse;
import com.frezo.qtht.entity.RoleMenu;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface RoleMenuMapper {

    RoleMenu toEntity(RoleMenuAssignRequest dto);

    @Mapping(source = "role.id", target = "roleId")
    @Mapping(source = "menu.id", target = "menuId")
    @Mapping(source = "menu.code", target = "menuCode")
    RoleMenuResponse toResponseDTO(RoleMenu roleMenu);
}
