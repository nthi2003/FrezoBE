package com.frezo.task.dto.response;

import com.frezo.task.common.PriorityEnum;
import com.frezo.task.common.TaskStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    private String id;
    private String title;
    private String projectId;
    private String assigneeId;
    private String description;
    private PriorityEnum priority;
    private TaskStatusEnum status;
    private LocalDateTime deadline;
    private List<TagResponse> tags;
    private String fileName;
    private String createdBy;
    private LocalDateTime createdDate;
}
