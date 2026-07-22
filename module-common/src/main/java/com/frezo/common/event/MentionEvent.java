package com.frezo.common.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

@Getter
public class MentionEvent extends ApplicationEvent {

    private final String commentId;
    private final String subjectType;
    private final String subjectId;
    private final String authorUsername;
    private final List<String> mentionedUserIds;
    private final String preview;

    public MentionEvent(Object source, String commentId, String subjectType, String subjectId,
                        String authorUsername, List<String> mentionedUserIds, String preview) {
        super(source);
        this.commentId = commentId;
        this.subjectType = subjectType;
        this.subjectId = subjectId;
        this.authorUsername = authorUsername;
        this.mentionedUserIds = mentionedUserIds;
        this.preview = preview;
    }
}
