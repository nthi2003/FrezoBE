package com.frezo.common.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogAuditRepository extends JpaRepository<AuditLogAudit, String> {
}
