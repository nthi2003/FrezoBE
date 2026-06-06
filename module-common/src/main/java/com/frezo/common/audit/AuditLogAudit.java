package com.frezo.common.audit;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLogAudit extends BaseEntity {

    @Column(name = "action", nullable = false)
    private String action; // READ, CREATE, UPDATE, DELETE, EXPORT

    @Column(name = "entity_name")
    private String entityName; // Tên bảng hoặc entity (e.g., Customer, NCC)
    
    @Column(name = "entity_id")
    private String entityId;
    
    @Column(name = "summary", length = 500)
    private String summary; // Nội dung chi tiết (Ví dụ: "Truy cập số điện thoại / update mã")

    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;

}
