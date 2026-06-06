package com.frezo.qtht.entity;

import com.frezo.common.constant.BlockReason;
import com.frezo.common.constant.TimeBlock;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "block_ip")
@AllArgsConstructor
@NoArgsConstructor
public class BlockIP {
    @Id
    @UuidGenerator
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "ip_address", nullable = false, length = 50)
    private String ipAddress;

    @Column(name = "blocked_until", nullable = false)
    private LocalDateTime blockedUntil;

    @Column(name = "block_level")
    private TimeBlock blockLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", length = 50)
    private BlockReason reason;

    @Column(name = "target_username")
    private String targetUserName;

    @Column(name = "failed_attempts")
    private Integer failedAttempts;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "updated_date")
    private LocalDateTime updatedDate;
}
