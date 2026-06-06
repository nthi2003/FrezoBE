package com.frezo.qtht.mapper;

import com.frezo.qtht.dto.request.MenuSaveRequest;
import com.frezo.qtht.dto.response.MenuResponse;
import com.frezo.qtht.dto.response.MenuSidebarResponse;
import com.frezo.qtht.entity.Menu;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MenuMapper {

    @Mapping(target = "permissions", ignore = true)
    MenuResponse toResponse(Menu menu);

    @IterableMapping(qualifiedByName = "toSidebarResponse")
    List<MenuResponse> toSidebarResponseList(List<Menu> menus);

    @Named("toSidebarResponse")
    @Mapping(target = "permissions", ignore = true)
    MenuResponse toSidebarResponse(Menu menu);

    @Mapping(source = "feUrl", target = "feUrl")
    @Mapping(source = "parentCode", target = "parentCode")
    MenuSidebarResponse toSidebarLightweight(Menu menu);

    List<MenuSidebarResponse> toSidebarLightweightList(List<Menu> menus);

    List<MenuResponse> toResponseList(List<Menu> menus);

    Menu toEntity(MenuSaveRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Menu menu, MenuSaveRequest request);
}
