package com.frezo.task.controller;

import com.frezo.task.dto.request.TaskRequest;
import com.frezo.task.dto.response.TaskResponse;
import com.frezo.task.service.TaskService;
import com.frezo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/task/task")
@RequiredArgsConstructor
@Tag(name = "Task API", description = "Task Management APIs")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    @Operation(summary = "Create a new task")
    public ApiResponse<TaskResponse> create(@RequestBody TaskRequest request) {
        return ApiResponse.ok(taskService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing task")
    public ApiResponse<TaskResponse> update(@PathVariable String id, @RequestBody TaskRequest request) {
        return ApiResponse.ok(taskService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task by ID")
    public ApiResponse<Void> delete(@PathVariable String id) {
        return ApiResponse.ok(taskService.delete(id));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a task by ID")
    public ApiResponse<TaskResponse> findById(@PathVariable String id) {
        return ApiResponse.ok(taskService.findById(id));
    }

    @GetMapping
    @Operation(summary = "Get all tasks")
    public ApiResponse<List<TaskResponse>> findAll() {
        return ApiResponse.ok(taskService.findAll());
    }

    @PatchMapping("/{id}/assign/{assigneeId}")
    @Operation(summary = "Assign a task to a user")
    public ApiResponse<TaskResponse> assign(@PathVariable String id, @PathVariable String assigneeId) {
        return ApiResponse.ok(taskService.assignTask(id, assigneeId));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update task status")
    public ApiResponse<TaskResponse> updateStatus(@PathVariable String id, @RequestParam String status) {
        return ApiResponse.ok(taskService.updateStatus(id, status));
    }
}
