package com.frezo.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Cấu hình ghi nhật ký API toàn hệ thống ({@code frezo.api-log.*}).
 */
@Data
@ConfigurationProperties(prefix = "frezo.api-log")
public class ApiLogProperties {

    /** Bật/tắt ghi log HTTP request. */
    private boolean enabled = true;

    /**
     * Aspect legacy (ghi trùng với Filter) — mặc định TẮT.
     * Chỉ bật khi debug / rollback khẩn.
     */
    private boolean aspectEnabled = false;

    /** Ghi request/response body (đã mask field nhạy cảm). */
    private boolean includeBody = true;

    /** Persist async qua {@code @Async} để không chặn request thread. */
    private boolean async = true;

    /** Giới hạn độ dài body lưu DB (ký tự). */
    private int maxBodyLength = 4000;

    /**
     * Prefix path (sau context-path) bị loại trừ — heartbeat / actuator / swagger…
     */
    private List<String> excludePathPrefixes = new ArrayList<>(List.of(
            "/actuator",
            "/swagger",
            "/swagger-ui",
            "/v3/api-docs",
            "/api-docs",
            "/favicon",
            "/error",
            "/ws-endpoint",
            "/auth/session/heartbeat",
            "/qtht/api-log"
    ));
}
