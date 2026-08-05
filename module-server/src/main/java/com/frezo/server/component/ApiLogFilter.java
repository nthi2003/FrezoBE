package com.frezo.server.component;

import com.frezo.qtht.entity.ApiLog;
import com.frezo.qtht.service.IpWhitelistService;
import com.frezo.server.config.ApiLogProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Ghi nhật ký API toàn hệ thống (mọi account, mọi endpoint trừ noise).
 * <p>
 * Username được lấy <b>sau</b> {@code filterChain} — lúc JWT đã authenticate.
 * Persist 1 lần / request (async nếu {@code frezo.api-log.async=true}).
 * Body được mask field nhạy cảm (password, token, secret…).
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class ApiLogFilter extends OncePerRequestFilter {

    private static final Pattern SENSITIVE_JSON = Pattern.compile(
            "(?i)(\"(?:password|passwd|pwd|secret|token|accessToken|refreshToken|authorization|apiKey|api_key|clientSecret)\"\\s*:\\s*)\"([^\"]*)\"");

    private final ApiLogWriter apiLogWriter;
    private final ApiLogProperties apiLogProperties;
    private final com.frezo.qtht.service.IpBlacklistService ipBlacklistService;
    private final IpWhitelistService ipWhitelistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip = getClientIp(request);
        String path = normalizePath(request);

        // Check whitelist first (if whitelist is not empty, only allow whitelisted IPs)
        if (ipWhitelistService.hasWhitelistEnabled() && !ipWhitelistService.isWhitelisted(ip)) {
            if (!path.startsWith("/auth/") && !path.startsWith("/actuator")
                    && !path.startsWith("/swagger") && !path.startsWith("/v3/api-docs")) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write("{\"status\": 403, \"message\": \"IP của bạn không được phép truy cập hệ thống.\"}");
                return;
            }
        }

        if (ipBlacklistService.isBanned(ip)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"status\": 403, \"message\": \"IP của bạn đã bị cấm truy cập hệ thống.\"}");
            return;
        }

        boolean shouldLog = apiLogProperties.isEnabled() && !isExcluded(path);

        long startTime = System.currentTimeMillis();
        LocalDateTime effFrom = LocalDateTime.now();

        HttpServletRequest requestForChain = request;
        CachedBodyHttpServletRequest cachedRequest = null;
        if (shouldLog && shouldCacheRequestBody(request)) {
            try {
                cachedRequest = new CachedBodyHttpServletRequest(request);
                requestForChain = cachedRequest;
            } catch (Exception e) {
                log.warn("Cannot cache request body for {} — fallback to raw request", path);
            }
        }

        ContentCachingResponseWrapper wrappedResponse = shouldLog
                ? new ContentCachingResponseWrapper(response)
                : null;

        try {
            filterChain.doFilter(requestForChain, wrappedResponse != null ? wrappedResponse : response);
        } finally {
            if (shouldLog && wrappedResponse != null) {
                try {
                    persistLog(request, path, ip, effFrom, startTime, cachedRequest, wrappedResponse);
                } catch (Exception e) {
                    log.error("Error building API log for {}", path, e);
                }
                try {
                    wrappedResponse.copyBodyToResponse();
                } catch (Exception e) {
                    log.error("Error copying response body", e);
                }
            }
        }
    }

    private void persistLog(
            HttpServletRequest request,
            String path,
            String ip,
            LocalDateTime effFrom,
            long startTime,
            CachedBodyHttpServletRequest cachedRequest,
            ContentCachingResponseWrapper wrappedResponse) {

        long duration = System.currentTimeMillis() - startTime;
        LocalDateTime effTo = LocalDateTime.now();
        int status = wrappedResponse.getStatus();

        String requestBody = apiLogProperties.isIncludeBody()
                ? maskSensitive(extractRequestBody(cachedRequest, request))
                : null;
        String responseBody = apiLogProperties.isIncludeBody()
                ? maskSensitive(extractResponseBody(wrappedResponse))
                : null;

        String query = request.getQueryString();
        if (query != null && query.length() > 2000) {
            query = query.substring(0, 2000);
        }

        String ua = request.getHeader("User-Agent");
        if (ua != null && ua.length() > 512) {
            ua = ua.substring(0, 512);
        }

        String errorMessage = null;
        if (status >= 400) {
            errorMessage = extractErrorMessage(responseBody, status);
        }

        ApiLog apiLog = ApiLog.builder()
                .uri(path)
                .method(request.getMethod())
                .ipAddress(ip)
                .username(resolveUsername())
                .statusCode(status)
                .duration(duration)
                .requestBody(requestBody)
                .responseBody(responseBody)
                .effFrom(effFrom)
                .effTo(effTo)
                .userAgent(ua)
                .queryString(query)
                .module(resolveModule(path))
                .errorMessage(errorMessage)
                .traceId(resolveTraceId(request, wrappedResponse))
                .build();

        if (apiLogProperties.isAsync()) {
            apiLogWriter.saveAsync(apiLog);
        } else {
            apiLogWriter.saveSync(apiLog);
        }
    }

    private String resolveUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return "ANONYMOUS";
            }
            Object principal = auth.getPrincipal();
            if (principal instanceof UserDetails ud) {
                return ud.getUsername();
            }
            String name = auth.getName();
            if (name == null || name.isBlank() || "anonymousUser".equalsIgnoreCase(name)) {
                return "ANONYMOUS";
            }
            return name;
        } catch (Exception e) {
            return "ANONYMOUS";
        }
    }

    private String resolveModule(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return "root";
        }
        String p = path.startsWith("/") ? path.substring(1) : path;
        int slash = p.indexOf('/');
        String segment = slash < 0 ? p : p.substring(0, slash);
        return segment.isBlank() ? "root" : segment.toLowerCase(Locale.ROOT);
    }

    private String resolveTraceId(HttpServletRequest request, ContentCachingResponseWrapper response) {
        String[] headers = {"X-Request-Id", "X-Trace-Id", "X-Correlation-Id", "traceparent"};
        for (String h : headers) {
            String v = request.getHeader(h);
            if (v != null && !v.isBlank()) {
                return v.length() > 64 ? v.substring(0, 64) : v;
            }
        }
        String fromRes = response.getHeader("X-Request-Id");
        if (fromRes != null && !fromRes.isBlank()) {
            return fromRes.length() > 64 ? fromRes.substring(0, 64) : fromRes;
        }
        return null;
    }

    private String extractErrorMessage(String responseBody, int status) {
        if (responseBody == null || responseBody.isBlank()) {
            return "HTTP " + status;
        }
        // Cố gắng lấy "message" từ JSON response chuẩn Frezo
        java.util.regex.Matcher m = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]{1,400})\"").matcher(responseBody);
        if (m.find()) {
            return m.group(1);
        }
        String trimmed = responseBody.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

    private String maskSensitive(String body) {
        if (body == null || body.isBlank()) return body;
        return SENSITIVE_JSON.matcher(body).replaceAll("$1\"***\"");
    }

    private boolean shouldCacheRequestBody(HttpServletRequest req) {
        if (!apiLogProperties.isIncludeBody()) return false;
        String method = req.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return false;
        }
        String ct = req.getContentType();
        if (ct != null && ct.toLowerCase(Locale.ROOT).startsWith("multipart/")) {
            return false;
        }
        int len = req.getContentLength();
        return len != 0;
    }

    private String extractRequestBody(CachedBodyHttpServletRequest cachedReq, HttpServletRequest rawReq) {
        if (cachedReq != null) {
            byte[] bytes = cachedReq.getCachedBody();
            if (bytes == null || bytes.length == 0) {
                return isBodylessMethod(rawReq.getMethod()) ? null : "[empty body]";
            }
            return toBodyString(bytes, cachedReq.getCharacterEncoding());
        }
        String ct = rawReq.getContentType();
        if (ct != null && ct.toLowerCase(Locale.ROOT).startsWith("multipart/")) {
            return "[multipart upload — body skipped, size=" + rawReq.getContentLength() + " bytes]";
        }
        return null;
    }

    private String extractResponseBody(ContentCachingResponseWrapper wrappedResponse) {
        byte[] resBytes = wrappedResponse.getContentAsByteArray();
        if (resBytes.length == 0) return null;
        return toBodyString(resBytes, wrappedResponse.getCharacterEncoding());
    }

    private String toBodyString(byte[] bytes, String charset) {
        try {
            String cs = (charset == null || charset.isBlank()) ? StandardCharsets.UTF_8.name() : charset;
            String body = new String(bytes, cs);
            int max = apiLogProperties.getMaxBodyLength();
            if (body.length() > max) {
                return body.substring(0, max)
                        + "\n\n...[truncated — total " + body.length() + " chars]";
            }
            return body;
        } catch (Exception e) {
            return "[error parsing body: " + e.getMessage() + "]";
        }
    }

    private boolean isBodylessMethod(String method) {
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /** Path không gồm context-path (/api). */
    private String normalizePath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        if (uri.isEmpty()) return "/";
        return uri;
    }

    private boolean isExcluded(String path) {
        if (path == null) return true;
        for (String prefix : apiLogProperties.getExcludePathPrefixes()) {
            if (prefix != null && !prefix.isBlank() && path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // IP whitelist/blacklist vẫn chạy; chỉ skip wrap khi disabled — nhưng doFilterInternal
        // luôn cần chạy cho IP checks. Không skip filter hoàn toàn.
        return false;
    }
}
