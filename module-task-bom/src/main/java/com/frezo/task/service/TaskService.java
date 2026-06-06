package com.frezo.task.service;

import com.frezo.task.dto.request.TaskRequest;
import com.frezo.task.dto.response.TaskResponse;
import com.frezo.util.web.Response;

import java.util.List;

public interface TaskService {
    Response<TaskResponse> create(TaskRequest request);

    Response<TaskResponse> update(String id, TaskRequest request);

    Response<Void> delete(String id);

    Response<TaskResponse> findById(String id);

    Response<List<TaskResponse>> findAll();

    Response<TaskResponse> assignTask(String taskId, String assigneeId);

    Response<TaskResponse> updateStatus(String taskId, String status);
}
