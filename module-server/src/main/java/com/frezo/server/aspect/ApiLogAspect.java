package com.frezo.server.aspect;

import com.frezo.common.helper.SystemUtils;
import com.frezo.qtht.entity.ApiLog;
import com.frezo.qtht.service.ApiLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * Legacy aspect — ghi log trùng với {@link com.frezo.server.component.ApiLogFilter}.
 * <p>
 * Mặc định <b>TẮT</b> ({@code frezo.api-log.aspect-enabled=false}).
 * Chỉ bật khi cần rollback khẩn / so sánh.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "frezo.api-log", name = "aspect-enabled", havingValue = "true")
public class ApiLogAspect {

    private final ApiLogService apiLogService;

    @Around("execution(* com.frezo.*.controller..*(..))")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return joinPoint.proceed();
        }
        HttpServletRequest request = attrs.getRequest();

        Long startTimeAttr = (Long) request.getAttribute("startTime");
        long startTime = startTimeAttr != null ? startTimeAttr : System.currentTimeMillis();

        String ip = (String) request.getAttribute("ipAddress");
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isBlank()) {
                ip = request.getRemoteAddr();
            }
        }

        String username = SystemUtils.getCurrentUsername();
        String uri = request.getRequestURI();
        String method = request.getMethod();

        Object result;
        int statusCode = 200;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            statusCode = 500;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            LocalDateTime now = LocalDateTime.now();

            ApiLog apiLog = ApiLog.builder()
                    .uri(uri)
                    .method(method)
                    .ipAddress(ip)
                    .username(username != null ? username : "ANONYMOUS")
                    .statusCode(statusCode)
                    .duration(duration)
                    .requestBody("")
                    .responseBody("")
                    .effFrom(now)
                    .effTo(now)
                    .build();

            try {
                apiLogService.saveLog(apiLog);
            } catch (Exception ex) {
                log.warn("ApiLogAspect save failed: {}", ex.getMessage());
            }
        }
    }
}
