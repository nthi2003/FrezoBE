package com.frezo.auth.entity;

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
@Table(name = "user_session")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession extends BaseEntity {

    @Column(name = "username", length = 100, nullable = false)
    private String username;

    @Column(name = "token", length = 500)
    private String token;

    @Column(name = "refresh_token", length = 500)
    private String refreshToken;

    @Column(name = "ip_address", length = 100)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "login_time")
    private LocalDateTime loginTime;

    @Column(name = "last_active_time")
    private LocalDateTime lastActiveTime;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoked_by")
    private String revokedBy;
}

