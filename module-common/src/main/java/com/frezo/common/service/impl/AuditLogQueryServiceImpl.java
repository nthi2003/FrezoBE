package com.frezo.common.service.impl;

import com.frezo.common.entity.AuditLog;
import com.frezo.common.repository.AuditLogRepository;
import com.frezo.common.service.AuditLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuditLogQueryServiceImpl implements AuditLogQueryService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> search(int page, int size, String keyword) {
        int safePage = Math.max(0, page);
        int safeSize = Math.max(1, Math.min(size, 100));
        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by("timestamp").descending());

        if (StringUtils.hasText(keyword)) {
            String k = keyword.trim();
            return auditLogRepository.findByUsernameContainingOrActionContaining(k, k, pageRequest);
        }
        return auditLogRepository.findAll(pageRequest);
    }
}
