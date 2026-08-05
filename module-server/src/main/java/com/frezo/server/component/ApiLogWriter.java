package com.frezo.server.component;

import com.frezo.qtht.entity.ApiLog;
import com.frezo.qtht.service.ApiLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Persist {@link ApiLog} — tách bean riêng để {@code @Async} hoạt động (self-invocation không proxy).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiLogWriter {

    private final ApiLogService apiLogService;

    @Async
    public void saveAsync(ApiLog apiLog) {
        saveSync(apiLog);
    }

    public void saveSync(ApiLog apiLog) {
        try {
            apiLogService.saveLog(apiLog);
        } catch (Exception e) {
            log.error("Failed to persist API log uri={} method={}", apiLog.getUri(), apiLog.getMethod(), e);
        }
    }
}
