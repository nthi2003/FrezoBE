package com.frezo.qtht.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * CMS hướng dẫn (Docs Hub) — FR-DOC-03.
 * User thường chỉ thấy bài {@code published=true}; Admin/BA CRUD toàn bộ.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "guide",
        uniqueConstraints = @UniqueConstraint(name = "uk_guide_slug", columnNames = "slug")
)
public class Guide extends BaseEntity {

    /** Mã đường dẫn ổn định, vd. {@code guide-qlts}. */
    @Column(name = "slug", length = 120, nullable = false)
    private String slug;

    @Column(name = "title", length = 255, nullable = false)
    private String title;

    /** Nội dung Markdown (hoặc HTML đã sanitize). */
    @Column(name = "body", nullable = false, columnDefinition = "text")
    private String body;

    /** Nhóm module: Nhân sự / Tài sản / Duyệt / … */
    @Column(name = "module", length = 100)
    private String module;

    /** Mô tả 1 câu trên list hub. */
    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "sort_order")
    private Integer sortOrder;

    /** Chỉ bài đã xuất bản hiện với user thường. */
    @Column(name = "published", nullable = false)
    private Boolean published;
}
