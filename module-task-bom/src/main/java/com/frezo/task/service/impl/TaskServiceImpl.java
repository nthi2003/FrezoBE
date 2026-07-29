package com.frezo.task.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.task.common.TaskErrorCode;
import com.frezo.task.common.TaskStatusEnum;
import com.frezo.task.dto.request.TaskRequest;
import com.frezo.task.dto.response.TaskResponse;
import com.frezo.task.entity.Tag;
import com.frezo.task.entity.Task;
import com.frezo.task.mapper.TaskMapper;
import com.frezo.task.repository.TagRepository;
import com.frezo.task.repository.TaskRepository;
import com.frezo.task.service.TaskService;
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
    public TaskResponse create(TaskRequest request) {
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
        return taskMapper.toResponse(savedTask);
    }

    @Override
    @Transactional
    public TaskResponse update(String id, TaskRequest request) {
        Task task = findEntityById(id);
        taskMapper.updateEntity(request, task);
        if (request.getTagIds() != null) {
            List<Tag> tags = tagRepository.findAllById(request.getTagIds());
            task.setTags(tags);
        }
        Task savedTask = taskRepository.save(task);
        return taskMapper.toResponse(savedTask);
    }

    @Override
    @Transactional
    public Void delete(String id) {
        Task task = findEntityById(id);
        task.setIsDeleted(true);
        taskRepository.save(task);
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse findById(String id) {
        Task task = findEntityById(id);
        return taskMapper.toResponse(task);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> findAll() {
        List<Task> tasks = taskRepository.findAll().stream()
                .filter(t -> t.getIsDeleted() == null || !t.getIsDeleted())
                .collect(Collectors.toList());
        return taskMapper.toResponseList(tasks);
    }

    @Override
    @Transactional
    public TaskResponse assignTask(String taskId, String assigneeId) {
        Task task = findEntityById(taskId);
        task.setAssigneeId(assigneeId);
        Task savedTask = taskRepository.save(task);
        return taskMapper.toResponse(savedTask);
    }

    @Override
    @Transactional
    public TaskResponse updateStatus(String taskId, String status) {
        Task task = findEntityById(taskId);
        try {
            task.setStatus(TaskStatusEnum.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new AppException(TaskErrorCode.INVALID_STATUS, status);
        }
        Task savedTask = taskRepository.save(task);
        return taskMapper.toResponse(savedTask);
    }

    private Task findEntityById(String id) {
        return taskRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(TaskErrorCode.TASK_NOT_FOUND, id));
    }
}
