package com.frezo.qtbv.entity;

import com.frezo.common.domain.BaseEntity;
import com.frezo.qtbv.common.ReactionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "article_reactions")
public class ArticleReaction extends BaseEntity {

    @Column(name = "article_id", nullable = false, length = 50)
    private String articleId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReactionType type;
}
