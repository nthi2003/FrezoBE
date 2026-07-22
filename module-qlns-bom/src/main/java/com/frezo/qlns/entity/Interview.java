package com.frezo.qlns.entity;

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

import java.time.LocalDateTime;

/**
 * Buổi phỏng vấn — thuộc về 1 {@link JobApplication}. 1 application có thể có nhiều Interview
 * (vòng 1, vòng 2, technical, culture-fit, ...).
 */
@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "recr_interview", indexes = {
        @Index(name = "idx_intv_app", columnList = "application_id"),
        @Index(name = "idx_intv_status", columnList = "status")
})
public class Interview extends BaseEntity {

    @Column(name = "application_id", length = 36, nullable = false)
    private String applicationId;

    /** PHONE / ONLINE / ONSITE / TECHNICAL / HR / FINAL. */
    @Column(name = "type", length = 20, nullable = false)
    private String type;

    @Column(name = "scheduled_at", nullable = false)
    private LocalDateTime scheduledAt;

    @Column(name = "interviewer_username", length = 100)
    private String interviewerUsername;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    /** SCHEDULED / DONE / CANCELLED / NO_SHOW. */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    /** Điểm đánh giá 0–10 (bằng Double để cho phép nửa điểm). */
    @Column(name = "score")
    private Double score;

    @Column(name = "feedback", length = 2000)
    private String feedback;
}
