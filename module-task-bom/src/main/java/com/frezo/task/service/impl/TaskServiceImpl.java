package com.frezo.task.service.impl;

import com.frezo.common.exception.QTHTException;
import com.frezo.task.common.TaskStatusEnum;
import com.frezo.task.dto.request.TaskRequest;
import com.frezo.task.dto.response.TaskResponse;
import com.frezo.task.entity.Tag;
import com.frezo.task.entity.Task;
import com.frezo.task.mapper.TaskMapper;
import com.frezo.task.repository.TagRepository;
import com.frezo.task.repository.TaskRepository;
import com.frezo.task.service.TaskService;
import com.frezo.util.web.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TagRepository tagRepository;
    private final TaskMapper taskMapper;

    @Override
    @Transactional
    public Response<TaskResponse> create(TaskRequest request) {
        Task task = taskMapper.toEntity(request);
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(request.getTagIds());
            task.setTags(tags);
        }
        task.setIsDeleted(false);
        if (task.getStatus() == null) {
            task.setStatus(TaskStatusEnum.OPEN);
        }
        Task savedTask = taskRepository.save(task);
        return Response.ok(taskMapper.toResponse(savedTask));
    }

    @Override
    @Transactional
    public Response<TaskResponse> update(String id, TaskRequest request) {
        Task task = findEntityById(id);
        taskMapper.updateEntity(request, task);
        if (request.getTagIds() != null) {
            List<Tag> tags = tagRepository.findAllById(request.getTagIds());
            task.setTags(tags);
        }
        Task savedTask = taskRepository.save(task);
        return Response.ok(taskMapper.toResponse(savedTask));
    }

    @Override
    @Transactional
    public Response<Void> delete(String id) {
        Task task = findEntityById(id);
        task.setIsDeleted(true);
        taskRepository.save(task);
        return Response.ok();
    }

    @Override
    @Transactional(readOnly = true)
    public Response<TaskResponse> findById(String id) {
        Task task = findEntityById(id);
        return Response.ok(taskMapper.toResponse(task));
    }

    @Override
    @Transactional(readOnly = true)
    public Response<List<TaskResponse>> findAll() {
        List<Task> tasks = taskRepository.findAll().stream()
                .filter(t -> t.getIsDeleted() == null || !t.getIsDeleted())
                .collect(Collectors.toList());
        return Response.ok(taskMapper.toResponseList(tasks));
    }

    @Override
    @Transactional
    public Response<TaskResponse> assignTask(String taskId, String assigneeId) {
        Task task = findEntityById(taskId);
        task.setAssigneeId(assigneeId);
        Task savedTask = taskRepository.save(task);
        return Response.ok(taskMapper.toResponse(savedTask));
    }

    @Override
    @Transactional
    public Response<TaskResponse> updateStatus(String taskId, String status) {
        Task task = findEntityById(taskId);
        try {
            task.setStatus(TaskStatusEnum.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new QTHTException("INVALID_STATUS", "Trạng thái không hợp lệ: " + status);
        }
        Task savedTask = taskRepository.save(task);
        return Response.ok(taskMapper.toResponse(savedTask));
    }

    private Task findEntityById(String id) {
        return taskRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new QTHTException("TASK_NOT_FOUND", "Không tìm thấy công việc với ID: " + id));
    }
}
