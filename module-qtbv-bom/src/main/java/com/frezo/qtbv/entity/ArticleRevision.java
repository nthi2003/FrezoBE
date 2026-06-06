package com.frezo.qtbv.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "article_revisions")
public class ArticleRevision extends BaseEntity {

    @Column(name = "article_id", nullable = false, length = 50)
    private String articleId;

    @Column(name = "editor_id", nullable = false, length = 50)
    private String editorId;

    @Column(name = "old_content", columnDefinition = "TEXT")
    private String oldContent;

    @Column(name = "new_content", columnDefinition = "TEXT", nullable = false)
    private String newContent;
}
