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
 * LivestreamEvent — nhắc lịch livestream (standalone, không cần Meta App).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "mkt_livestream_events")
public class LivestreamEvent extends BaseEntity {

    @Column(name = "title", length = 500, nullable = false)
    private String title;

    /** FACEBOOK | YOUTUBE | TIKTOK | ZALO | OTHER */
    @Column(name = "channel", length = 32, nullable = false)
    @Builder.Default
    private String channel = "FACEBOOK";

    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    @Column(name = "duration_minutes")
    @Builder.Default
    private Integer durationMinutes = 60;

    /** Số phút trước giờ live để nhắc (mặc định 30). */
    @Column(name = "notify_before_minutes")
    @Builder.Default
    private Integer notifyBeforeMinutes = 30;

    /** SCHEDULED | LIVE | ENDED | CANCELLED */
    @Column(name = "status", length = 16, nullable = false)
    @Builder.Default
    private String status = "SCHEDULED";

    @Column(name = "registrant_count")
    @Builder.Default
    private Integer registrantCount = 0;

    @Column(name = "notified_at")
    private OffsetDateTime notifiedAt;

    @Column(name = "stream_url", length = 1000)
    private String streamUrl;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;
}
