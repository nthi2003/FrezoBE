package com.frezo.qtht.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Nhật ký từng lần chạy của một {@link SystemJob}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "system_job_history")
public class SystemJobHistory extends BaseEntity {

    @Column(name = "job_code", length = 50, nullable = false)
    private String jobCode;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    /** RUNNING trong lúc chạy → SUCCESS | FAILED khi xong; SKIPPED khi bỏ lượt do đang chạy. */
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    /** SYSTEM (cron) hoặc username người bấm chạy tay. */
    @Column(name = "triggered_by", length = 100)
    private String triggeredBy;
}
