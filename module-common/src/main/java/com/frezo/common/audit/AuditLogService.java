package com.frezo.common.audit;

import com.frezo.common.helper.SystemUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogAuditRepository auditLogRepository;

    public void logAction(String action, String entityName, String entityId, String summary, HttpServletRequest request) {
        try {
            AuditLogAudit auditLog = AuditLogAudit.builder()
                    .action(action)
                    .entityName(entityName)
                    .entityId(entityId)
                    .summary(summary)
                    .ipAddress(request != null ? getClientIp(request) : "SYSTEM")
                    .userAgent(request != null ? request.getHeader("User-Agent") : "SYSTEM")
                    .build();

            auditLogRepository.save(auditLog);
            log.info("AuditLog: User [{}] performed {} on {} [{}]", 
                     SystemUtils.getCurrentUsername(), action, entityName, entityId);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage(), e);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        }
        return ipAddress;
    }
}
