package com.frezo.common.audit;

import com.frezo.common.helper.SystemUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditLogAspect {

    private final AuditLogAuditRepository auditLogRepository;

    @AfterReturning(pointcut = "@annotation(auditAction)", returning = "result")
    public void logAudit(JoinPoint joinPoint, AuditAction auditAction, Object result) {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String username = SystemUtils.getCurrentUsername();
            String ipAddress = SystemUtils.getClientIp(request);
            String userAgent = request.getHeader("User-Agent");

            AuditLogAudit auditLog = AuditLogAudit.builder()
                    .action(auditAction.action())
                    .entityName(auditAction.entity())
                    .summary(auditAction.value())
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();
            
            // Set auditing fields manually if JPA auditing is not picked up in this context
            auditLog.setCreatedBy(username != null ? username : "anonymous");
            auditLog.setCreatedDate(LocalDateTime.now());

            auditLogRepository.save(auditLog);
            
            log.info("Audit Log saved: {} - {} by {}", auditAction.action(), auditAction.value(), username);
        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }
}
