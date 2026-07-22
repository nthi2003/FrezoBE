package com.frezo.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Set MDC {@code traceId} + {@code user} cho MỌI request để log tracing.
 * <p>
 * Xem chi tiết: {@code FrezoBE/AI_BACKEND_ENGINEERING_GUIDE.md §8 — Logging}.
 * <p>
 * <b>Hành vi:</b>
 * <ul>
 *   <li>Đọc {@code X-Correlation-Id} header nếu client truyền (dùng cho distributed tracing), ngược lại sinh UUID mới.</li>
 *   <li>Set MDC:
 *     <ul>
 *       <li>{@code traceId} — correlation id</li>
 *       <li>{@code user} — username từ SecurityContext (set lại lần 2 sau khi JwtAuthenticationFilter chạy — hiện tại filter này chạy trước nên chỉ có sau khi auth OK, xem NOTE bên dưới)</li>
 *     </ul>
 *   </li>
 *   <li>Set response header {@code X-Correlation-Id} — FE có thể log/debug với id này.</li>
 *   <li>{@code finally} clear MDC — TRÁNH thread pool leak MDC context.</li>
 * </ul>
 * <p>
 * <b>Order:</b> {@code HIGHEST_PRECEDENCE + 10} — chạy TRƯỚC {@code JwtAuthenticationFilter}
 * để traceId có sẵn cho auth logs. Username sẽ được set trong filter chain sau đó qua wrapper hoặc re-check trong logback pattern.
 * <p>
 * <b>NOTE:</b> vì filter này chạy TRƯỚC auth filter, {@code user} MDC ở đầu request sẽ null. Để log có username,
 * có thể refactor thành 2 filter (trước + sau auth) hoặc dùng logback converter đọc SecurityContext runtime.
 * Approach hiện tại: log user null cho pre-auth logs (chấp nhận được).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String MDC_TRACE_ID = "traceId";
    public static final String MDC_USER = "user";
    public static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(HEADER_CORRELATION_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        } else if (traceId.length() > 64) {
            traceId = traceId.substring(0, 64);
        }

        try {
            MDC.put(MDC_TRACE_ID, traceId);
            resolveAndPutUser();
            response.setHeader(HEADER_CORRELATION_ID, traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_USER);
        }
    }

    /** Set MDC {@code user} nếu SecurityContext có Authentication. Chỉ có sau JwtAuthenticationFilter. */
    private void resolveAndPutUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            MDC.put(MDC_USER, auth.getName());
        }
    }
}
