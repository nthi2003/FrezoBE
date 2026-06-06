package com.frezo.common.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {
    
    @Column(name = "username", nullable = false)
    private String username;
    
    @Column(name = "title")
    private String title;
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "is_read")
    private Boolean isRead;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "type")
    private String type; // INFO, WARNING, SUCCESS, ERROR
}
