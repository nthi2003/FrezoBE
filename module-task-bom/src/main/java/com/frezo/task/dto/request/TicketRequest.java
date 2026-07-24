package com.frezo.task.dto.request;

import com.frezo.task.entity.Ticket;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TicketRequest {
    private String title;
    private String description;
    private Ticket.TicketStatus status;
    private Ticket.TicketPriority priority;
    private String category;
    private String assigneeId;
    private LocalDateTime dueDate;
    private String resolutionNote;
}
