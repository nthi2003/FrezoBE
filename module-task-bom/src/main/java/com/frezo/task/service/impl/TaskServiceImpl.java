package com.frezo.task.service.impl;

import com.frezo.common.exception.AppException;
import com.frezo.task.common.TaskErrorCode;
import com.frezo.task.common.TaskStatusEnum;
import com.frezo.task.dto.request.ReviewRequest;
import com.frezo.task.dto.request.TaskRequest;
import com.frezo.task.dto.response.TaskResponse;
import com.frezo.task.entity.Tag;
import com.frezo.task.entity.Task;
import com.frezo.task.mapper.TaskMapper;
import com.frezo.task.repository.TagRepository;
import com.frezo.task.repository.TaskRepository;
import com.frezo.task.security.TaskAccessHelper;
import com.frezo.task.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Task service — visibility + completion review.
 * <p>DONE = EU hoàn thành (chờ duyệt); CLOSED = người giao đã xác nhận.
 */
@Service
@RequiredArgsConstructor
public class TaskServiceImpl implements TaskService {

    private final TaskRepository taskRepository;
    private final TagRepository tagRepository;
    private final TaskMapper taskMapper;
    private final TaskAccessHelper accessHelper;

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
        if (task.getStatus() == TaskStatusEnum.CLOSED && !accessHelper.isAdmin()) {
            task.setStatus(TaskStatusEnum.OPEN);
        }
        Task savedTask = taskRepository.save(task);
        return toVisibleResponse(savedTask);
    }

    @Override
    @Transactional
    public TaskResponse update(String id, TaskRequest request) {
        Task task = requireVisibleTask(id);
        TaskStatusEnum oldStatus = task.getStatus();
        taskMapper.updateEntity(request, task);
        if (request.getTagIds() != null) {
            List<Tag> tags = tagRepository.findAllById(request.getTagIds());
            task.setTags(tags);
        }
        if (request.getStatus() != null && oldStatus != task.getStatus()) {
            enforceStatusTransition(task, oldStatus, task.getStatus());
        }
        Task savedTask = taskRepository.save(task);
        return toVisibleResponse(savedTask);
    }

    @Override
    @Transactional
    public Void delete(String id) {
        Task task = requireVisibleTask(id);
        if (!accessHelper.canReviewTask(task)) {
            throw new AppException(TaskErrorCode.TASK_ACCESS_DENIED);
        }
        task.setIsDeleted(true);
        taskRepository.save(task);
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public TaskResponse findById(String id) {
        return toVisibleResponse(requireVisibleTask(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TaskResponse> findAll() {
        return taskRepository.findAll().stream()
                .filter(t -> t.getIsDeleted() == null || !t.getIsDeleted())
                .filter(accessHelper::canViewTask)
                .map(this::toVisibleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public TaskResponse assignTask(String taskId, String assigneeId) {
        Task task = requireVisibleTask(taskId);
        if (!accessHelper.canReviewTask(task) && !accessHelper.isAdmin()) {
            throw new AppException(TaskErrorCode.TASK_ACCESS_DENIED);
        }
        task.setAssigneeId(assigneeId);
        Task savedTask = taskRepository.save(task);
        return toVisibleResponse(savedTask);
    }

    @Override
    @Transactional
    public TaskResponse updateStatus(String taskId, String status) {
        Task task = requireVisibleTask(taskId);
        TaskStatusEnum oldStatus = task.getStatus();
        TaskStatusEnum newStatus;
        try {
            newStatus = TaskStatusEnum.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException(TaskErrorCode.INVALID_STATUS, status);
        }
        enforceStatusTransition(task, oldStatus, newStatus);
        task.setStatus(newStatus);
        Task savedTask = taskRepository.save(task);
        return toVisibleResponse(savedTask);
    }

    @Override
    @Transactional
    public TaskResponse review(String id, ReviewRequest request) {
        Task task = requireVisibleTask(id);
        if (!accessHelper.canReviewTask(task)) {
            throw new AppException(TaskErrorCode.TASK_REVIEW_FORBIDDEN);
        }
        if (task.getStatus() != TaskStatusEnum.DONE) {
            throw new AppException(TaskErrorCode.TASK_REVIEW_INVALID);
        }
        boolean approved = request != null && request.isApproved();
        task.setStatus(approved ? TaskStatusEnum.CLOSED : TaskStatusEnum.IN_PROGRESS);
        Task saved = taskRepository.save(task);
        return toVisibleResponse(saved);
    }

    private void enforceStatusTransition(Task task, TaskStatusEnum from, TaskStatusEnum to) {
        if (from == to) return;
        if (accessHelper.isAdmin()) return;

        if (to == TaskStatusEnum.DONE) {
            if (!accessHelper.canCompleteTask(task)) {
                throw new AppException(TaskErrorCode.TASK_COMPLETE_FORBIDDEN);
            }
            return;
        }
        if (to == TaskStatusEnum.CLOSED) {
            if (!accessHelper.canReviewTask(task)) {
                throw new AppException(TaskErrorCode.TASK_REVIEW_FORBIDDEN);
            }
            if (from != TaskStatusEnum.DONE) {
                throw new AppException(TaskErrorCode.TASK_REVIEW_INVALID);
            }
            return;
        }
        if (from == TaskStatusEnum.DONE && to == TaskStatusEnum.IN_PROGRESS) {
            if (!accessHelper.canReviewTask(task)) {
                throw new AppException(TaskErrorCode.TASK_REVIEW_FORBIDDEN);
            }
            return;
        }
        if (from == TaskStatusEnum.CLOSED) {
            throw new AppException(TaskErrorCode.TASK_REVIEW_FORBIDDEN);
        }
    }

    private Task requireVisibleTask(String id) {
        Task task = taskRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new AppException(TaskErrorCode.TASK_NOT_FOUND, id));
        if (!accessHelper.canViewTask(task)) {
            throw new AppException(TaskErrorCode.TASK_ACCESS_DENIED);
        }
        return task;
    }

    private TaskResponse toVisibleResponse(Task task) {
        TaskResponse response = taskMapper.toResponse(task);
        boolean pending = task.getStatus() == TaskStatusEnum.DONE;
        response.setPendingReview(pending);
        response.setCanReview(pending && accessHelper.canReviewTask(task));
        response.setCanComplete(accessHelper.canCompleteTask(task));
        return response;
    }
}
