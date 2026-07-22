package com.frezo.qtht.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    private String id;
    private String content;
    private String authorId;
    private String authorName;
    private String authorAvatar;
    private List<String> mentions;
    private String parentId;
    private String createdAt;
    private String updatedAt;
    private Boolean deleted;
    private Boolean isSystem;
    private String systemAction;
}
