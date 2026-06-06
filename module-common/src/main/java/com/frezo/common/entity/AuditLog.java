package com.frezo.common.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "audit_logs")
public class AuditLog extends BaseEntity {

    @Column(name = "username")
    private String username;

    @Column(name = "action")
    private String action; // CREATE, UPDATE, DELETE

    @Column(name = "object_type")
    private String objectType; // e.g., USER, CONTRACT

    @Column(name = "object_id")
    private String objectId;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;
}
