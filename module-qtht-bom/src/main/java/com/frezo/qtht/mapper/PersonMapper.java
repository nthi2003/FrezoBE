package com.frezo.qtht.mapper;

import com.frezo.qtht.dto.request.PersonAddRequest;
import com.frezo.qtht.dto.request.PersonUpdateRequest;
import com.frezo.qtht.dto.response.PersonResponse;
import com.frezo.qtht.entity.Person;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface PersonMapper {
    @Mapping(source = "organization.name", target = "orgName")
    @Mapping(source = "department.name", target = "departmentName")
    PersonResponse toResponse(Person person);

    Person toEntity(PersonAddRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget Person entity, PersonUpdateRequest request);
}
