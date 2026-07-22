package com.frezo.qtht.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Config cho {@code @CheckPermission} aspect — bind từ {@code app.security.check-permission.*}.
 * <p>
 * Xem chi tiết: {@code FrezoBE/AI_BACKEND_ENGINEERING_GUIDE.md §7.2}.
 * <p>
 * <b>Ví dụ config:</b>
 * <pre>
 * app:
 *   security:
 *     check-permission:
 *       enabled: true                 # default: true. Chỉ tắt trong tình huống rollback khẩn.
 *       fail-open-on-error: false     # default: false. Nếu Aspect throw exception nội bộ → false = deny, true = allow.
 *       log-audit: true               # default: true. Log mọi permission check (allow / deny) để audit.
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "app.security.check-permission")
@Getter
@Setter
public class SecurityPermissionProperties {

    /**
     * Master switch cho {@code @CheckPermission}.
     * <p>
     * {@code true} (default): mọi endpoint annotate {@code @CheckPermission} bị check qua {@code permission} table.
     * {@code false}: aspect bypass hoàn toàn — CHỈ dùng trong dev / rollback khẩn.
     */
    private boolean enabled = true;

    /**
     * Nếu aspect gặp lỗi nội bộ (query DB fail, user không tìm thấy...) → allow hay deny?
     * <p>
     * {@code false} (default, secure): deny → throw {@code AppException(FORBIDDEN)}.
     * {@code true}: allow — chỉ dùng khi migration / permission table chưa seed đủ.
     */
    private boolean failOpenOnError = false;

    /**
     * Log mọi permission check (allow / deny) để audit — default true.
     * Tắt nếu volume log quá lớn (rare).
     */
    private boolean logAudit = true;
}
