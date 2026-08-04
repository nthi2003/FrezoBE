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

/**
 * CommentModerationRule — rule từ khoá để kiểm duyệt comment (MVP offline).
 * Khi có Meta webhook feed, engine sẽ match rule này; hiện user áp dụng thủ công trên queue.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "mkt_comment_rules")
public class CommentModerationRule extends BaseEntity {

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    /** Từ khoá, phân tách bởi dấu phẩy (case-insensitive contains). */
    @Column(name = "keywords", columnDefinition = "TEXT", nullable = false)
    private String keywords;

    /** HIDE | REPLY | FLAG */
    @Column(name = "action", length = 16, nullable = false)
    @Builder.Default
    private String action = "FLAG";

    @Column(name = "reply_template", columnDefinition = "TEXT")
    private String replyTemplate;

    @Column(name = "enabled")
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "hit_count")
    @Builder.Default
    private Long hitCount = 0L;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
