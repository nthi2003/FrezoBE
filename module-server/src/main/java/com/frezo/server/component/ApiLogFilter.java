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
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;

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

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        ApiLog apiLog = ApiLog.builder()
                .uri(request.getRequestURI())
                .method(request.getMethod())
                .ipAddress(getClientIp(request))
                .username(getCurrentUsername())
                .effFrom(effFrom)
                .build();

        // Lưu bản ghi lúc bắt đầu gọi (tạo record time call lúc)
        try {
            apiLogService.saveLog(apiLog);
        } catch (Exception e) {
            log.error("Error saving initial API log", e);
        }

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            LocalDateTime effTo = LocalDateTime.now();

            String requestBody = null;
            byte[] reqBytes = wrappedRequest.getContentAsByteArray();
            if (reqBytes.length > 0) {
                try {
                    String charset = wrappedRequest.getCharacterEncoding();
                    if (charset == null || charset.isBlank()) {
                        charset = StandardCharsets.UTF_8.name();
                    }
                    requestBody = new String(reqBytes, charset);
                    if (requestBody.length() > 2000)
                        requestBody = requestBody.substring(0, 2000) + "...[truncated]";
                } catch (Exception e) { requestBody = "[error parsing body]"; }
            }
            String responseBody = null;
            byte[] resBytes = wrappedResponse.getContentAsByteArray();
            if (resBytes.length > 0) {
                try {
                    String charset = wrappedResponse.getCharacterEncoding();
                    if (charset == null || charset.isBlank()) {
                        charset = StandardCharsets.UTF_8.name();
                    }
                    responseBody = new String(resBytes, charset);
                    if (responseBody.length() > 2000)
                        responseBody = responseBody.substring(0, 2000) + "...[truncated]";
                } catch (Exception e) { responseBody = "[error parsing body]"; }
            }

            // Cập nhật thông tin khi kết thúc call (nhưng call thì cũng phải lưu)
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

            wrappedResponse.copyBodyToResponse();
        }
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
