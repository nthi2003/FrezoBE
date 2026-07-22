package com.frezo.auth.service.impl.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * POJO utility — trích IP client từ {@link HttpServletRequest} theo chuỗi header phổ biến.
 * Không cần DI: lấy request qua {@link RequestContextHolder} để tránh phải inject
 * {@code HttpServletRequest} vào mọi service dùng chung.
 */
public final class IpResolver {

    private IpResolver() {}

    /**
     * Lấy IP client hiện tại. Trả {@code "0.0.0.0"} nếu không có request context
     * (VD gọi từ scheduler / async thread).
     */
    public static String currentClientIp() {
        HttpServletRequest req = currentRequest();
        return req == null ? "0.0.0.0" : resolveClientIp(req);
    }

    /** Lấy User-Agent header hiện tại — null-safe. */
    public static String currentUserAgent() {
        HttpServletRequest req = currentRequest();
        return req == null ? null : req.getHeader("User-Agent");
    }

    public static String resolveClientIp(HttpServletRequest request) {
        String ip = firstNonBlank(
                request.getHeader("X-Forwarded-For"),
                request.getHeader("Proxy-Client-IP"),
                request.getHeader("WL-Proxy-Client-IP"),
                request.getHeader("HTTP_CLIENT_IP"),
                request.getHeader("HTTP_X_FORWARDED_FOR"),
                request.getRemoteAddr()
        );
        // X-Forwarded-For có thể list nhiều IP: "client, proxy1, proxy2"
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private static HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes sra) {
            return sra.getRequest();
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isEmpty() && !"unknown".equalsIgnoreCase(v)) {
                return v;
            }
        }
        return null;
    }
}
