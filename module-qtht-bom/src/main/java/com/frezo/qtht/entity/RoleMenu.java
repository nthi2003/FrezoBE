package com.frezo.qtht.entity;

import com.frezo.common.domain.BaseEntity;
import lombok.*;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@DynamicInsert
@DynamicUpdate
@Table(name = "role_menu", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "role_id", "menu_id", "app_code" })
})
public class RoleMenu extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", referencedColumnName = "id", nullable = false)
    private Role role;

    @Column(name = "role_id", insertable = false, updatable = false)
    private String roleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", referencedColumnName = "id", nullable = false)
    private Menu menu;

    @Column(name = "menu_id", insertable = false, updatable = false)
    private String menuId;

    @Column(name = "app_code", length = 50, nullable = false)
    private String appCode;

    @Column(name = "action", nullable = false, columnDefinition = "varchar(255) default 'ALL'")
    @Builder.Default
    private String action = "ALL";

    // Lưu parent menu ID nếu menu được chọn là menu con
    @Column(name = "parent_menu_id")
    private String parentMenuId;
}
