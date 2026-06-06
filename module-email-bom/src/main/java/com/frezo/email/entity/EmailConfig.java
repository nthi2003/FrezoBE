package com.frezo.email.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "email_configs")
public class EmailConfig extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "api_key", length = 100, nullable = false)
    private String apiKey;

    @Column(nullable = false, unique = true, length = 100)
    private String smtp;

    private Short port;

    @Column(nullable = false, unique = true, length = 100)
    private String nameEmail;

    private String orgId;

    private Boolean activated;
}
