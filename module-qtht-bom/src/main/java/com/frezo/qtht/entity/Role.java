package com.frezo.qtht.entity;

import com.frezo.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "roles",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"app_code", "code"})
        }
)
public class Role extends BaseEntity {
    @Column(length = 50, nullable = false)
    private String code;     // ADMIN

    @Column(name = "app_code", length = 50, nullable = false)
    private String appCode;  // QTHT

    private String name;
    private String description;
    private String status;

}
