package com.frezo.auth.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "user_name", length = 40, nullable = false)
    private String userName;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "name", length = 500)
    private String name;

    @Column(name = "password", length = 50)
    private String password;

    @Column(name = "email", length = 50)
    private String email;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "data_action", length = 50)
    private Short dataAction;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "status")
    private Integer status;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "reset_key", length = 20)
    private String resetKey;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "reset_date")
    private LocalDateTime resetDate;

    @Basic(fetch = FetchType.LAZY)
    @Column(name = "person_id")
    private String personId;

    @Column(name = "requires_two_factor")
    private Boolean requiresTwoFactor;

    @Column(name = "otp_code", length = 10)
    private String otpCode;

    @Column(name = "otp_expiration")
    private LocalDateTime otpExpiration;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;
}
