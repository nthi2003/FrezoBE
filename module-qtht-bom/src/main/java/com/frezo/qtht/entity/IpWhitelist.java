package com.frezo.qtht.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ip_whitelist")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpWhitelist extends BaseEntity {

    @Column(name = "ip_address", length = 100, nullable = false, unique = true)
    private String ipAddress;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "is_active")
    private Boolean isActive;
}

