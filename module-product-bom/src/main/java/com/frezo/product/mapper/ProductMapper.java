package com.frezo.product.mapper;

import com.frezo.product.dto.request.ProductCreateRequest;
import com.frezo.product.dto.request.ProductUpdateRequest;
import com.frezo.product.dto.response.ProductResponse;
import com.frezo.product.entity.Product;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, builder = @org.mapstruct.Builder(disableBuilder = true))
public interface ProductMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isActive", constant = "true")
    Product toEntity(ProductCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void updateEntity(ProductUpdateRequest request, @MappingTarget Product product);

    ProductResponse toResponse(Product product);
}
