package com.frezo.auth.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * JWT config — bind từ {@code app.security.jwt.*}.
 * <p>
 * Xem chi tiết: {@code FrezoBE/AI_BACKEND_ENGINEERING_GUIDE.md §7.1}.
 * <p>
 * <b>Bắt buộc set env var {@code APP_SECURITY_JWT_SECRET}</b> — nếu missing hoặc &lt; 64 bytes → app fail-fast startup
 * ({@code @Validated} sẽ throw {@code BindException} khi context refresh).
 * <p>
 * KHÔNG có default value cho {@code secret} (cấm hardcode fallback trong code).
 * <p>
 * <b>Sinh secret an toàn:</b>
 * <pre>
 * openssl rand -base64 64
 * # hoặc Java:
 * java -jar tools.jar generate-secret
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "app.security.jwt")
@Getter
@Setter
@Validated
public class JwtProperties {

    /**
     * HMAC secret key. Tối thiểu 64 bytes cho HS512. KHÔNG hardcode default — bắt buộc set env.
     */
    @NotBlank(message = "JWT secret must be provided via APP_SECURITY_JWT_SECRET env var (min 64 bytes)")
    @Size(min = 64, message = "JWT secret must be at least 64 bytes for HS512 (received: {actualLength})")
    private String secret;

    /** Access token TTL (ms). Default: 1 giờ. */
    @Min(value = 60_000, message = "JWT expiration must be at least 60 seconds")
    private long expiration = 3_600_000L;

    /** Refresh token TTL (ms). Default: 7 ngày. */
    @Min(value = 60_000, message = "JWT refresh expiration must be at least 60 seconds")
    private long refreshExpiration = 604_800_000L;

    /** Token issuer claim. */
    private String issuer = "frezo";

    /** Token audience claim (optional). */
    private String audience = "frezo-app";
}
