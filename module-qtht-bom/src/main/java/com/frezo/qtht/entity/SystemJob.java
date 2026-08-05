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
 * Cấu hình một tác vụ nền — cron + enable/disable + kết quả lần chạy gần nhất.
 * Nguồn sự thật cho {@code DynamicJobScheduler} (cron trong DB thắng default của bean).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "system_job")
public class SystemJob extends BaseEntity {

    @Column(name = "job_code", length = 50, nullable = false)
    private String jobCode;

    @Column(name = "job_name", length = 200, nullable = false)
    private String jobName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "module_code", length = 50)
    private String moduleCode;

    @Column(name = "cron_expression", length = 100, nullable = false)
    private String cronExpression;

    @Column(name = "enabled")
    private Boolean enabled;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    /** SUCCESS | FAILED | SKIPPED — null khi chưa chạy lần nào. */
    @Column(name = "last_status", length = 20)
    private String lastStatus;

    @Column(name = "last_duration_ms")
    private Long lastDurationMs;

    @Column(name = "last_message", columnDefinition = "TEXT")
    private String lastMessage;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;
}
