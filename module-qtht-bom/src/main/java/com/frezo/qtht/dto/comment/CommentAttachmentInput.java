package com.frezo.qtht.dto.comment;

import lombok.Data;

@Data
public class CommentAttachmentInput {
    private String url;
    private String name;
    private String contentType;
    private Long size;
    private String objectName;
}
