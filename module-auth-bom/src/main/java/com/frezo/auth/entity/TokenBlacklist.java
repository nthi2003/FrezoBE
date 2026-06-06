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
@Table(name = "token_blacklist")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenBlacklist extends BaseEntity {

    @Column(name = "token", length = 500, nullable = false)
    private String token;

    @Column(name = "username", length = 100, nullable = false)
    private String username;

    @Column(name = "blacklisted_at")
    private LocalDateTime blacklistedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}

