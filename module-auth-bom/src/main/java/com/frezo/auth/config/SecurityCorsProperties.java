package com.frezo.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * CORS configuration properties — bind từ {@code app.cors.*} trong application.yml / env.
 * <p>
 * Xem chi tiết: {@code FrezoBE/AI_BACKEND_ENGINEERING_GUIDE.md §7.1}.
 * <p>
 * <b>Ví dụ config:</b>
 * <pre>
 * app:
 *   cors:
 *     allowed-origins:
 *       - https://app.frezo.vn
 *       - https://admin.frezo.vn
 *     allowed-methods: [GET, POST, PUT, PATCH, DELETE, OPTIONS]
 *     allowed-headers: [Authorization, Content-Type, Accept-Language, X-Correlation-Id, Idempotency-Key]
 *     exposed-headers: [Authorization, X-Correlation-Id, X-RateLimit-Remaining, X-RateLimit-Reset]
 *     allow-credentials: true
 *     max-age: 3600
 * </pre>
 * <p>
 * Env override (12-factor):
 * <pre>
 * APP_CORS_ALLOWED-ORIGINS=https://app.frezo.vn,https://admin.frezo.vn
 * </pre>
 * <p>
 * <b>CẤM</b> {@code allowed-origins: ["*"]} production. Dev/local có thể dùng
 * {@code ["http://localhost:5173", "http://localhost:3000"]}.
 */
@Component
@ConfigurationProperties(prefix = "app.cors")
@Getter
@Setter
public class SecurityCorsProperties {

    /** Whitelist origins. KHÔNG dùng {@code "*"} cho production. */
    private List<String> allowedOrigins = List.of(
            "http://localhost:5173",
            "http://localhost:3000",
            "http://localhost:4200"
    );

    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private List<String> allowedHeaders = List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Accept-Language",
            "X-Correlation-Id",
            "X-Client-Version",
            "Idempotency-Key",
            "If-Match"
    );

    private List<String> exposedHeaders = List.of(
            "Authorization",
            "X-Correlation-Id",
            "X-RateLimit-Limit",
            "X-RateLimit-Remaining",
            "X-RateLimit-Reset",
            "Retry-After",
            "Location"
    );

    private boolean allowCredentials = true;

    /** Preflight cache TTL (giây). */
    private long maxAge = 3600L;
}
