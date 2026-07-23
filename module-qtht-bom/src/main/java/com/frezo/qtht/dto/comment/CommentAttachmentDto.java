package com.frezo.qtht.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentAttachmentDto {
    private String id;
    private String url;
    private String name;
    private String contentType;
    private Long size;
    private String objectName;
}
