package com.frezo.fbautomation.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.time.OffsetDateTime;

/**
 * PageReview — theo dõi đánh giá fanpage / Google / Shopee (MVP nhập tay).
 * Alert khi rating &lt;= 2 sao (status NEW + lowRating flag).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "mkt_page_reviews")
public class PageReview extends BaseEntity {

    @Column(name = "platform", length = 32, nullable = false)
    @Builder.Default
    private String platform = "FACEBOOK";

    /** 1–5 */
    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "author_name", length = 255)
    private String authorName;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /** NEW | ACKNOWLEDGED | REPLIED | ARCHIVED */
    @Column(name = "status", length = 16, nullable = false)
    @Builder.Default
    private String status = "NEW";

    @Column(name = "reply_text", columnDefinition = "TEXT")
    private String replyText;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "external_url", length = 1000)
    private String externalUrl;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
