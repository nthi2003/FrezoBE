package com.frezo.qtht.mapper;

import com.frezo.qtht.dto.request.SettingAddRequest;
import com.frezo.qtht.dto.request.SettingEditRequest;
import com.frezo.qtht.dto.response.SettingResponse;
import com.frezo.qtht.entity.Setting;
import org.mapstruct.*;

@Mapper(componentModel = "spring" , unmappedTargetPolicy =  ReportingPolicy.IGNORE)
public interface SettingMapper {

    SettingResponse toResponse(Setting setting);

    Setting toEntity(SettingAddRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Setting entity , SettingEditRequest request);
}
