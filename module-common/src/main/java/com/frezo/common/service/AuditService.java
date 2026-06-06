package com.frezo.common.service;

import com.frezo.common.entity.AuditLog;
import com.frezo.common.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(String username, String action, String objectType, String objectId, String oldValue, String newValue, String ip, String userAgent) {
        AuditLog auditLog = AuditLog.builder()
                .username(username)
                .action(action)
                .objectType(objectType)
                .objectId(objectId)
                .oldValue(oldValue)
                .newValue(newValue)
                .ipAddress(ip)
                .userAgent(userAgent)
                .timestamp(LocalDateTime.now())
                .build();
        auditLogRepository.save(auditLog);
    }
}
