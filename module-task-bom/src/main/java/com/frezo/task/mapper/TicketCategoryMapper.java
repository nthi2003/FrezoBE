package com.frezo.task.mapper;

import com.frezo.task.dto.request.TicketCategoryRequest;
import com.frezo.task.dto.response.TicketCategoryResponse;
import com.frezo.task.entity.TicketCategory;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TicketCategoryMapper {

    TicketCategoryResponse toResponse(TicketCategory entity);

    List<TicketCategoryResponse> toResponseList(List<TicketCategory> entities);

    TicketCategory toEntity(TicketCategoryRequest request);

    void updateEntity(TicketCategoryRequest request, @MappingTarget TicketCategory entity);
}
