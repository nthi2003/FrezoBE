package com.frezo.customer.mapper;

import com.frezo.customer.dto.request.CustomerRequest;
import com.frezo.customer.dto.response.CustomerResponse;
import com.frezo.customer.entity.Customer;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomerMapper {

    @Mapping(target = "phoneLast4", source = "phoneLast4")
    CustomerResponse toResponse(Customer customer);

    @Mapping(target = "phoneEncrypted", ignore = true)
    @Mapping(target = "phoneHash", ignore = true)
    @Mapping(target = "phoneLast4", ignore = true)
    Customer toEntity(CustomerRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "phoneEncrypted", ignore = true)
    @Mapping(target = "phoneHash", ignore = true)
    @Mapping(target = "phoneLast4", ignore = true)
    void updateEntity(@MappingTarget Customer entity, CustomerRequest request);
}
