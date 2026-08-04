package com.frezo.task.service;

import com.frezo.task.dto.request.ReviewRequest;
import com.frezo.task.dto.request.TaskRequest;
import com.frezo.task.dto.response.TaskResponse;

import java.util.List;

public interface TaskService {
    TaskResponse create(TaskRequest request);

    TaskResponse update(String id, TaskRequest request);

    Void delete(String id);

    TaskResponse findById(String id);

    List<TaskResponse> findAll();

    TaskResponse assignTask(String taskId, String assigneeId);

    TaskResponse updateStatus(String taskId, String status);

    /** Người giao / admin duyệt DONE → CLOSED hoặc trả về IN_PROGRESS. */
    TaskResponse review(String id, ReviewRequest request);
}
