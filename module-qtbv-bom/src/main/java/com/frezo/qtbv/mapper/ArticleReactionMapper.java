package com.frezo.qtbv.mapper;

import com.frezo.qtbv.dto.response.ArticleReactionResponse;
import com.frezo.qtbv.entity.ArticleReaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE, builder = @org.mapstruct.Builder(disableBuilder = true))
public interface ArticleReactionMapper {
    @Mapping(target = "createdAt", source = "createdDate")
    ArticleReactionResponse toDto(ArticleReaction entity);
}
