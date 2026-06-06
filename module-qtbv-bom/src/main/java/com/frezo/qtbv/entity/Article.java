package com.frezo.qtbv.entity;

import com.frezo.common.domain.BaseEntity;
import com.frezo.qtbv.common.ArticleStatus;
import com.frezo.qtbv.common.PublishScope;
import com.frezo.qtht.entity.Organization;
import com.frezo.qtht.entity.Person;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "articles")
public class Article extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "author_id", nullable = false, length = 50)
    private String authorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Person author;

    @Column(name = "manager_id", length = 50)
    private String managerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Organization organization;

    @Column(name = "organization_id", length = 50)
    private String organizationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ArticleStatus status = ArticleStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "publish_scope", nullable = false, length = 20)
    @Builder.Default
    private PublishScope publishScope = PublishScope.INTERNAL;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = false;

    @Column(name = "is_public")
    @Builder.Default
    private Boolean isPublic = false;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

}
