package com.frezo.qtht.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "ip_blacklist")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpBlacklist extends BaseEntity {
    private String ipAddress;
    private String reason;
    private String bannedBy;
    private LocalDateTime bannedUntil; // To support hour/day based ban
    private Boolean active; // If false, the IP is unbanned
}
