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
    private String category;
    private String categoryName;
    private String reporterId;
    private String assigneeId;
    private LocalDateTime dueDate;
    private LocalDateTime resolvedAt;
    private String resolutionNote;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    /** Aggregate user comments (non-system, not deleted) — backfilled on list/detail. */
    private Integer commentCount;
    /** Aggregate comment attachments on this ticket. */
    private Integer attachmentCount;
    /** RESOLVED = chờ người giao / admin duyệt. */
    private Boolean pendingReview;
    /** Current user được duyệt (reporter / admin). */
    private Boolean canReview;
    /** Current user là assignee (hoặc admin). */
    private Boolean canComplete;
}
