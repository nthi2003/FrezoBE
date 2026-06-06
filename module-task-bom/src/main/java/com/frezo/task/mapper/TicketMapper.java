package com.frezo.task.mapper;

import com.frezo.task.dto.request.TicketRequest;
import com.frezo.task.dto.response.TicketResponse;
import com.frezo.task.entity.Ticket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TicketMapper {

    Ticket toEntity(TicketRequest request);

    TicketResponse toResponse(Ticket entity);

    List<TicketResponse> toResponseList(List<Ticket> entities);

    void updateEntityFromRequest(TicketRequest request, @MappingTarget Ticket entity);
}
