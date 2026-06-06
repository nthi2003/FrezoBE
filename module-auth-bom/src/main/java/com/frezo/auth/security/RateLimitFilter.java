package com.frezo.auth.security;

import com.frezo.common.ratelimit.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {
    private final RateLimitService rateLimitService;
    private final JwtTokenProvider jwtTokenProvider;
    private static final String RATE_LIMIT_EXCEEDED = "error.rate.limit.exceeded";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String ip = getClientIp(request);

        // Layer 1: IP check (chặn DDoS)
        if (!rateLimitService.tryConsumeByIp(ip)) {
            sendTooManyRequests(response, "IP rate limit exceeded");
            return;
        }

        // Layer 2: User/plan check (nếu đã authenticate)
        String token = resolveToken(request);
        if (token != null && jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsernameFromJWT(token);
            Boolean isAdmin = jwtTokenProvider.getIsAdminFromJWT(token);
            if (!rateLimitService.tryConsumeByUser(username, isAdmin != null && isAdmin)) {
                sendTooManyRequests(response, "User rate limit exceeded");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendTooManyRequests(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String body = "{\"success\":false,\"message\":\"" + msg + "\",\"data\":null}";
        response.getWriter().write(body);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For"); // sau nginx/proxy
        if (ip == null || ip.isBlank()) ip = request.getRemoteAddr();
        return ip.split(",")[0].trim(); // lấy IP đầu tiên nếu có chain proxy
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) return bearer.substring(7);
        return null;
    }
}
