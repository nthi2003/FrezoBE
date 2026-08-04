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
 * ModeratedComment — hàng đợi comment cần kiểm duyệt (nhập tay / import; chưa webhook Meta).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "mkt_moderated_comments")
public class ModeratedComment extends BaseEntity {

    @Column(name = "platform", length = 32, nullable = false)
    @Builder.Default
    private String platform = "FACEBOOK";

    @Column(name = "author_name", length = 255)
    private String authorName;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "post_url", length = 1000)
    private String postUrl;

    /** PENDING | HIDDEN | REPLIED | IGNORED | FLAGGED */
    @Column(name = "status", length = 16, nullable = false)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "matched_rule_id", length = 36)
    private String matchedRuleId;

    @Column(name = "matched_rule_name", length = 255)
    private String matchedRuleName;

    @Column(name = "reply_text", columnDefinition = "TEXT")
    private String replyText;

    @Column(name = "commented_at")
    private OffsetDateTime commentedAt;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
