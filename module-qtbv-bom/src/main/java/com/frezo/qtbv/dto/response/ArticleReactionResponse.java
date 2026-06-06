package com.frezo.qtbv.dto.response;

import com.frezo.qtbv.common.ReactionType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleReactionResponse {
    private String id;
    private String articleId;
    private String userId;
    private ReactionType type;
    private LocalDateTime createdAt;
}
