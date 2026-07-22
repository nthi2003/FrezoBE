package com.frezo.server.component;

import com.frezo.qtht.entity.ApiLog;
import com.frezo.qtht.service.ApiLogService;
import com.frezo.qtht.service.IpWhitelistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;

/** Giới hạn kích thước body log (ký tự) — tránh persist blob quá lớn xuống DB. */
class ApiLogConstants {
    static final int MAX_BODY_LEN = 4000;
}

/// ThiNVQ  Filter - bắt IP + wrap request/response
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class ApiLogFilter extends OncePerRequestFilter {

    private final ApiLogService apiLogService;
    private final com.frezo.qtht.service.IpBlacklistService ipBlacklistService;
    private final IpWhitelistService ipWhitelistService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String ip = getClientIp(request);

        // Check whitelist first (if whitelist is not empty, only allow whitelisted IPs)
        if (ipWhitelistService.hasWhitelistEnabled() && !ipWhitelistService.isWhitelisted(ip)) {
            // Allow auth endpoints even if not whitelisted (to prevent lockout)
            String path = request.getRequestURI();
            if (!path.startsWith("/auth/") && !path.startsWith("/actuator") && !path.startsWith("/swagger") && !path.startsWith("/v3/api-docs")) {
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

        long startTime = System.currentTimeMillis();
        LocalDateTime effFrom = LocalDateTime.now();

        // ---- Wrap request để cache body ----
        // Với multipart (file upload), KHÔNG dùng cached wrapper để tránh copy file lớn.
        // Với GET/HEAD/DELETE/OPTIONS thì body thường trống nên cũng skip để đỡ overhead.
        HttpServletRequest requestForChain = request;
        CachedBodyHttpServletRequest cachedRequest = null;
        if (shouldCacheRequestBody(request)) {
            try {
                cachedRequest = new CachedBodyHttpServletRequest(request);
                requestForChain = cachedRequest;
            } catch (Exception e) {
                log.warn("Cannot cache request body for {} — fallback to raw request", request.getRequestURI());
            }
        }

        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        ApiLog apiLog = ApiLog.builder()
                .uri(request.getRequestURI())
                .method(request.getMethod())
                .ipAddress(getClientIp(request))
                .username(getCurrentUsername())
                .effFrom(effFrom)
                .build();

        // Lưu bản ghi lúc bắt đầu gọi — vẫn tồn tại record kể cả khi request crash
        try {
            apiLogService.saveLog(apiLog);
        } catch (Exception e) {
            log.error("Error saving initial API log", e);
        }

        try {
            filterChain.doFilter(requestForChain, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            LocalDateTime effTo = LocalDateTime.now();

            // ---- Extract request body ----
            // Với cached wrapper thì body luôn có sẵn (không phụ thuộc downstream đã đọc chưa).
            String requestBody = extractRequestBody(cachedRequest, request);

            // ---- Extract response body ----
            String responseBody = extractResponseBody(wrappedResponse);

            apiLog.setStatusCode(wrappedResponse.getStatus());
            apiLog.setDuration(duration);
            apiLog.setRequestBody(requestBody);
            apiLog.setResponseBody(responseBody);
            apiLog.setEffTo(effTo);

            try {
                apiLogService.saveLog(apiLog);
            } catch (Exception e) {
                log.error("Error updating API log", e);
            }

            try {
                wrappedResponse.copyBodyToResponse();
            } catch (Exception e) {
                log.error("Error copying response body", e);
            }
        }
    }

    /**
     * Có nên eager-cache request body không?
     * - GET/HEAD/DELETE/OPTIONS: body thường trống → skip
     * - multipart/form-data: file upload lớn → skip để tránh double memory
     * - Content-Length = 0: chắc chắn không có body → skip
     */
    private boolean shouldCacheRequestBody(HttpServletRequest req) {
        String method = req.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method)) {
            return false;
        }
        String ct = req.getContentType();
        if (ct != null && ct.toLowerCase().startsWith("multipart/")) {
            return false;
        }
        // Với chunked transfer, contentLength = -1 → vẫn cần cache
        int len = req.getContentLength();
        return len != 0;
    }

    private String extractRequestBody(CachedBodyHttpServletRequest cachedReq, HttpServletRequest rawReq) {
        // Ưu tiên cached bytes vì chúng ta biết chắc đã đọc đủ
        if (cachedReq != null) {
            byte[] bytes = cachedReq.getCachedBody();
            if (bytes == null || bytes.length == 0) {
                return isBodylessMethod(rawReq.getMethod()) ? null : "[empty body]";
            }
            return toBodyString(bytes, cachedReq.getCharacterEncoding());
        }
        // Multipart hoặc method không có body — chỉ note metadata
        String ct = rawReq.getContentType();
        if (ct != null && ct.toLowerCase().startsWith("multipart/")) {
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
            if (body.length() > ApiLogConstants.MAX_BODY_LEN) {
                return body.substring(0, ApiLogConstants.MAX_BODY_LEN)
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

    private String getCurrentUsername() {
        try {
            return SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getName();
        } catch (Exception e) {
            return "anonymous";
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator")
                || path.startsWith("/swagger")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/favicon");
    }
}
