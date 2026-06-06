package com.frezo.task.mapper;

import com.frezo.task.dto.request.TagRequest;
import com.frezo.task.dto.response.TagResponse;
import com.frezo.task.entity.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TagMapper {

    TagResponse toResponse(Tag tag);

    List<TagResponse> toResponseList(List<Tag> tags);

    Tag toEntity(TagRequest request);

    void updateEntity(TagRequest request, @MappingTarget Tag tag);

}
