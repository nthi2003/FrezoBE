package com.frezo.qtbv.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleRevisionResponse {
    private String id;
    private String articleId;
    private String editorId;
    private String oldContent;
    private String newContent;
    private LocalDateTime createdAt;
}