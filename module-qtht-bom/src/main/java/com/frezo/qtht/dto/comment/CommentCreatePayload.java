package com.frezo.qtht.dto.comment;

import lombok.Data;

import java.util.List;

@Data
public class CommentCreatePayload {
    private String subjectType;
    private String subjectId;
    private String content;
    private String parentId;
    private List<String> mentionedUserIds;
    private List<CommentAttachmentInput> attachments;
}
