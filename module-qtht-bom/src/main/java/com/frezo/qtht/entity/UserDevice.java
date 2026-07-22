package com.frezo.qtht.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Push notification device — 1 nhân viên có thể có nhiều device (điện thoại + iPad).
 * expoPushToken là unique để tránh duplicate khi user reinstall app.
 * Dùng cho Expo Push API (https://exp.host/--/api/v2/push/send).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_device",
       uniqueConstraints = @UniqueConstraint(columnNames = "expo_push_token"))
public class UserDevice extends BaseEntity {

    @Column(name = "username", length = 100, nullable = false)
    private String username;

    @Column(name = "expo_push_token", length = 200, nullable = false)
    private String expoPushToken;

    /** ios | android | web */
    @Column(name = "platform", length = 20)
    private String platform;

    /** Model điện thoại (iPhone 15, Pixel 8, ...). */
    @Column(name = "device_name", length = 100)
    private String deviceName;

    /** UUID device để de-dup khi user cài lại app. */
    @Column(name = "device_id", length = 100)
    private String deviceId;

    @Column(name = "app_version", length = 20)
    private String appVersion;

    @Column(name = "last_active_at")
    private LocalDateTime lastActiveAt;

    @Column(name = "is_active")
    private Boolean isActive;
}
