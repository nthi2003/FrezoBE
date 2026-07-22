package com.frezo.approval.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ApprovalDecidedEvent extends ApplicationEvent {

    private final String requestId;
    private final String subjectType;
    private final String subjectId;
    private final String status;
    private final String actedBy;
    private final String comment;

    public ApprovalDecidedEvent(Object source, String requestId, String subjectType,
                                String subjectId, String status, String actedBy, String comment) {
        super(source);
        this.requestId = requestId;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.status = status;
        this.actedBy = actedBy;
        this.comment = comment;
    }
}
