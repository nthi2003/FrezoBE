package com.frezo.task.dto.response;

import com.frezo.task.entity.Ticket;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TicketResponse {
    private String id;
    private String code;
    private String title;
    private String description;
    private Ticket.TicketStatus status;
    private Ticket.TicketPriority priority;
    private Ticket.TicketCategory category;
    private String reporterId;
    private String assigneeId;
    private LocalDateTime dueDate;
    private LocalDateTime resolvedAt;
    private String resolutionNote;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
