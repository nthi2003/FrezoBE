package com.frezo.fbautomation.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "fb_accounts")
public class FacebookAccount extends BaseEntity {

    @Column(name = "username", length = 255, nullable = false, unique = true)
    private String username;

    @Column(name = "password", length = 500, nullable = false)
    private String password;

    @Column(name = "cookie", columnDefinition = "TEXT")
    private String cookie;

    @Column(name = "proxy_ip", length = 255)
    private String proxyIp;

    @Column(name = "status", length = 50, nullable = false)
    private String status;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "posts_today", columnDefinition = "INTEGER DEFAULT 0")
    private Integer postsToday;
}
