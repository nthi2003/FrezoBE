package com.frezo.task.mapper;

import com.frezo.common.mapper.CentralMapperConfig;
import com.frezo.task.dto.request.TaskRequest;
import com.frezo.task.dto.response.TaskResponse;
import com.frezo.task.entity.Task;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(config = CentralMapperConfig.class, uses = { TagMapper.class })
public interface TaskMapper {

    TaskResponse toResponse(Task task);

    List<TaskResponse> toResponseList(List<Task> tasks);

    @Mapping(target = "tags", ignore = true)
    Task toEntity(TaskRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "tags", ignore = true)
    void updateEntity(TaskRequest request, @MappingTarget Task task);

}
