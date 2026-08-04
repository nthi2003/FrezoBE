package com.frezo.common.service;

import com.frezo.common.entity.AuditLog;
import org.springframework.data.domain.Page;

/**
 * Query API cho audit log (đọc) — tách khỏi {@code AuditLogService} ghi log.
 */
public interface AuditLogQueryService {

    Page<AuditLog> search(int page, int size, String keyword);
}
