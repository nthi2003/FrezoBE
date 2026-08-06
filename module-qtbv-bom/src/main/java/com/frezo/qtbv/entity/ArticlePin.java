package com.frezo.qtbv.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "article_pins", uniqueConstraints = @UniqueConstraint(
        name = "uk_article_pin_org_article",
        columnNames = {"organization_id", "article_id"}
))
public class ArticlePin extends BaseEntity {

    @Column(name = "article_id", nullable = false, length = 50)
    private String articleId;

    @Column(name = "organization_id", nullable = false, length = 50)
    private String organizationId;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
