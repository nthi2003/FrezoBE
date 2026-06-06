package com.frezo.qtbv.mapper;

import com.frezo.qtbv.dto.request.CategoryRequest;
import com.frezo.qtbv.dto.response.CategoryResponse;
import com.frezo.qtbv.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

    @Mapping(target = "activated", source = "active")
    CategoryResponse toResponse(Category category);

    List<CategoryResponse> toResponseList(List<Category> categories);

    @Mapping(target = "active", source = "activated")
    Category toEntity(CategoryRequest request);

    @Mapping(target = "active", source = "activated")
    void updateEntity(CategoryRequest request, @MappingTarget Category category);
}
