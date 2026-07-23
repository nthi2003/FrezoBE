package com.frezo.task.mapper;

import com.frezo.common.mapper.CentralMapperConfig;
import com.frezo.task.dto.request.TicketRequest;
import com.frezo.task.dto.response.TicketResponse;
import com.frezo.task.entity.Ticket;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * Ticket mapping — update dùng IGNORE null để PUT partial không xoá
 * assignee/priority/category/dueDate khi client chỉ gửi một phần field.
 */
@Mapper(config = CentralMapperConfig.class)
public interface TicketMapper {

    Ticket toEntity(TicketRequest request);

    TicketResponse toResponse(Ticket entity);

    List<TicketResponse> toResponseList(List<Ticket> entities);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(TicketRequest request, @MappingTarget Ticket entity);
}
