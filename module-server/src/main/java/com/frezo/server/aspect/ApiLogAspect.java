package com.frezo.server.aspect;

import com.frezo.common.helper.SystemUtils;
import com.frezo.qtht.entity.ApiLog;
import com.frezo.qtht.service.ApiLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;
import java.time.LocalDateTime;

/// THINVQ : Tính duration , goi service save log ^^
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiLogAspect {

    private final ApiLogService apiLogService;

    @Around("execution(* com.frezo.*.controller..*(..))")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {

        HttpServletRequest request = ((ServletRequestAttributes)
                RequestContextHolder.getRequestAttributes()).getRequest();

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

        Object result = null;
        int statusCode = 200;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            statusCode = 500;
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            LocalDateTime effFrom = LocalDateTime.now();
            LocalDateTime effTo = LocalDateTime.now();

            ApiLog apiLog = ApiLog.builder()
                    .uri(uri)
                    .method(method)
                    .ipAddress(ip)
                    .username(username)
                    .statusCode(statusCode)
                    .duration(duration)
                    .requestBody("")
                    .responseBody("")
                    .effFrom(effFrom)
                    .effTo(effTo)
                    .build();

            apiLogService.saveLog(apiLog);
        }
    }
}
