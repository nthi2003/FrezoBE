package com.frezo.auth.entity;

import com.frezo.common.domain.BaseEntity;
import lombok.*;

import jakarta.persistence.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_role", uniqueConstraints = {
                @UniqueConstraint(columnNames = { "user_id", "role_id" })
})
public class UserRole extends BaseEntity {

        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", nullable = false)
        private User user;

        @Column(name = "user_id", insertable = false, updatable = false)
        private String userId;

        @Column(name = "role_id", nullable = true)
        private String roleId;

        private Integer status;
}
