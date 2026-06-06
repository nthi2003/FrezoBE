package com.frezo.task.mapper;

import com.frezo.task.dto.request.TaskRequest;
import com.frezo.task.dto.response.TaskResponse;
import com.frezo.task.entity.Task;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring", uses = { TagMapper.class }, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TaskMapper {

    TaskResponse toResponse(Task task);

    List<TaskResponse> toResponseList(List<Task> tasks);

    @Mapping(target = "tags", ignore = true)
    Task toEntity(TaskRequest request);

    @Mapping(target = "tags", ignore = true)
    void updateEntity(TaskRequest request, @MappingTarget Task task);

}
