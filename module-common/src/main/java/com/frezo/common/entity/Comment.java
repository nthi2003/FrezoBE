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
@Table(name = "app_comment", indexes = {
        @Index(name = "idx_cmt_subject", columnList = "subject_type,subject_id")
})
public class Comment extends BaseEntity {

    @Column(name = "subject_type", length = 40, nullable = false)
    private String subjectType;

    @Column(name = "subject_id", length = 36, nullable = false)
    private String subjectId;

    @Column(name = "content", length = 4000, nullable = false)
    private String content;

    @Column(name = "author_id", length = 36, nullable = false)
    private String authorId;

    @Column(name = "author_username", length = 100)
    private String authorUsername;

    @Column(name = "author_name", length = 255)
    private String authorName;

    @Column(name = "author_avatar", length = 500)
    private String authorAvatar;

    @Column(name = "parent_id", length = 36)
    private String parentId;

    @Column(name = "is_system")
    private Boolean isSystem;

    @Column(name = "system_action", length = 50)
    private String systemAction;
}
