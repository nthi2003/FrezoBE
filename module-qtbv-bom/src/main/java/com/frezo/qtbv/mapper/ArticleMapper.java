package com.frezo.qtbv.mapper;

import com.frezo.qtbv.dto.request.ArticleCreateRequest;
import com.frezo.qtbv.dto.request.ArticleUpdateRequest;
import com.frezo.qtbv.dto.response.ArticleResponse;
import com.frezo.qtbv.entity.Article;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, builder = @org.mapstruct.Builder(disableBuilder = true))
public interface ArticleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "managerId", source = "managerId")
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Article toEntityFromCreateRequest(ArticleCreateRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "code", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "isActive", ignore = true)
    @Mapping(target = "publishedAt", ignore = true)
    @Mapping(target = "managerId", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromRequest(ArticleUpdateRequest request, @MappingTarget Article article);

    @Mapping(target = "createdAt", source = "createdDate")
    @Mapping(target = "updatedAt", source = "updatedDate")
    @Mapping(target = "heartCount", ignore = true)
    @Mapping(target = "authorName", source = "author.name")
    @Mapping(target = "organizationName", source = "organization.name")
    ArticleResponse toDto(Article article);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    Article toEntity(ArticleResponse response);
}
