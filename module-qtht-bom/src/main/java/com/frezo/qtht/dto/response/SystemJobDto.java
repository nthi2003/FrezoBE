package com.frezo.qtht.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Một tác vụ nền kèm trạng thái runtime cho màn hình quản trị. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemJobDto {

    private String code;
    private String name;
    private String description;
    private String moduleCode;
    private String cronExpression;

    /** Diễn giải cron tiếng Việt — VD "12:00 mỗi ngày". */
    private String cronDescription;

    private Boolean enabled;

    /** ENABLED | DISABLED | RUNNING | ERROR. */
    private String status;

    private LocalDateTime lastRunAt;

    /** SUCCESS | FAILED | SKIPPED | null. */
    private String lastStatus;

    private Long lastDurationMs;
    private String lastMessage;
    private LocalDateTime nextRunAt;
}
