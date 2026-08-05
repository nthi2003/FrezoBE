package com.frezo.qtht.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Một lần chạy của tác vụ nền. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemJobHistoryDto {

    private String id;
    private String jobCode;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Long durationMs;

    /** SUCCESS | FAILED | SKIPPED. */
    private String status;

    private String message;

    /** SYSTEM hoặc username. */
    private String triggeredBy;
}
