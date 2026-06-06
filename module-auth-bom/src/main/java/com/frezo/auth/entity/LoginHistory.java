package com.frezo.auth.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "login_history")
public class LoginHistory extends BaseEntity {
    
    @Column(name = "user_name", nullable = false)
    private String userName;
    
    @Column(name = "ip_address")
    private String ipAddress;
    
    @Column(name = "user_agent")
    private String userAgent;
    
    @Column(name = "login_time")
    private LocalDateTime loginTime;
    
    @Column(name = "status")
    private String status; // SUCCESS, FAILED, 2FA_REQUIRED
}
