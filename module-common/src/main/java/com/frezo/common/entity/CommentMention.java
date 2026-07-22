package com.frezo.common.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "app_comment_mention", indexes = {
        @Index(name = "idx_cmt_mention_comment", columnList = "comment_id"),
        @Index(name = "idx_cmt_mention_user", columnList = "mentioned_user_id")
})
public class CommentMention extends BaseEntity {

    @Column(name = "comment_id", length = 36, nullable = false)
    private String commentId;

    @Column(name = "mentioned_user_id", length = 36, nullable = false)
    private String mentionedUserId;

    @Column(name = "mentioned_username", length = 100)
    private String mentionedUsername;
}
