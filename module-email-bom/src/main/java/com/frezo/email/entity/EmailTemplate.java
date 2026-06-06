package com.frezo.email.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "email_templates")
public class EmailTemplate extends BaseEntity {
    private String name;

    private String code;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String description;
}
