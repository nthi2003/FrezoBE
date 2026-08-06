package com.frezo.qtbv.dto.response;

import com.frezo.qtbv.common.ArticleContentType;
import com.frezo.qtbv.common.ArticleStatus;
import com.frezo.qtbv.common.PublishScope;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleResponse {
    private String id;
    private String code;
    private String title;
    private String content;
    private String summary;
    private String thumbnailUrl;
    private String type;
    private String categoryId;
    private String categoryName;
    private String categoryColor;
    private ArticleContentType contentType;
    private String externalUrl;
    private Boolean displayOnNews;
    private String authorId;
    private String authorName;
    private String managerId;
    private String organizationId;
    private String organizationName;
    private ArticleStatus status;
    private PublishScope publishScope;
    private Boolean isActive;
    private Boolean isPublic;
    private String rejectNote;
    private LocalDateTime publishedAt;
    private Long heartCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
