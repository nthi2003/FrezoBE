package com.frezo.auth.service.impl.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public final class IpResolver {

    private IpResolver() {}

    public static String currentClientIp() {
        HttpServletRequest req = currentRequest();
        return req == null ? "0.0.0.0" : resolveClientIp(req);
    }


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
