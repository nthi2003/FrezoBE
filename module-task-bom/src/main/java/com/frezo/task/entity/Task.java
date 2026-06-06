package com.frezo.task.entity;

import com.frezo.common.domain.BaseEntity;
import com.frezo.task.common.PriorityEnum;
import com.frezo.task.common.TaskStatusEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tasks")
public class Task extends BaseEntity {

    @Column(name = "title")
    private String title;

    @Column(name = "project_id")
    private String projectId;

    @Column(name = "assignee_id")
    private String assigneeId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private PriorityEnum priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TaskStatusEnum status;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @ManyToMany
    @JoinTable(name = "task_tags", joinColumns = @JoinColumn(name = "task_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private List<Tag> tags;

    @Column(name = "file_name")
    private String fileName;

}
