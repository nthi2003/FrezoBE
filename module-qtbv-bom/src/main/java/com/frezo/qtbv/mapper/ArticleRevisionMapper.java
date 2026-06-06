package com.frezo.qtbv.mapper;

import com.frezo.qtbv.dto.response.ArticleRevisionResponse;
import com.frezo.qtbv.entity.ArticleRevision;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", builder = @org.mapstruct.Builder(disableBuilder = true))
public interface ArticleRevisionMapper {
    @Mapping(target = "createdAt", source = "createdDate")
    ArticleRevisionResponse toDto(ArticleRevision entity);
}
