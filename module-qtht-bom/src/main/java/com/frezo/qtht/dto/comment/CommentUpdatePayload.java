package com.frezo.qtht.dto.comment;

import lombok.Data;

import java.util.List;

@Data
public class CommentUpdatePayload {
    private String content;
    private List<String> mentionedUserIds;
}
