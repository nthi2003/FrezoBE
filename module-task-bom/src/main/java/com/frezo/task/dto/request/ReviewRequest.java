package com.frezo.task.dto.request;

import lombok.Data;

/**
 * Manager/assigner review sau khi EU đánh dấu hoàn thành.
 * {@code approved=true} → CLOSED; {@code false} → trả lại IN_PROGRESS (ticket) / OPEN (task).
 */
@Data
public class ReviewRequest {
    private boolean approved;
    private String note;
}
