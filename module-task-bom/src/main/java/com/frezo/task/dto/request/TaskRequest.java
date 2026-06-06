package com.frezo.task.dto.request;

import com.frezo.task.common.PriorityEnum;
import com.frezo.task.common.TaskStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TaskRequest {
    private String title;
    private String projectId;
    private String assigneeId;
    private String description;
    private PriorityEnum priority;
    private TaskStatusEnum status;
    private LocalDateTime deadline;
    private List<String> tagIds;
    private String fileName;
}
