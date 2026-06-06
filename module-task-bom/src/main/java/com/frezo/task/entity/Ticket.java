package com.frezo.task.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tickets")
public class Ticket extends BaseEntity {

    @Column(name = "code", unique = true, length = 30)
    private String code; // TICKET-0001

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private TicketStatus status; // OPEN, IN_PROGRESS, RESOLVED, CLOSED

    @Enumerated(EnumType.STRING)
    @Column(name = "priority")
    private TicketPriority priority; // LOW, MEDIUM, HIGH, URGENT

    @Enumerated(EnumType.STRING)
    @Column(name = "category")
    private TicketCategory category; // BUG, FEATURE_REQUEST, SUPPORT, OTHER

    @Column(name = "reporter_id", length = 100)
    private String reporterId; // người tạo ticket

    @Column(name = "assignee_id", length = 100)
    private String assigneeId; // người được giao xử lý

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    public enum TicketStatus { OPEN, IN_PROGRESS, RESOLVED, CLOSED }
    public enum TicketPriority { LOW, MEDIUM, HIGH, URGENT }
    public enum TicketCategory { BUG, FEATURE_REQUEST, SUPPORT, OTHER }
}
