package com.frezo.qtht.entity;

import com.frezo.common.domain.BaseEntity;
import lombok.*;
import jakarta.persistence.*;

/**
 * MenuPermission - Bảng trung gian liên kết Menu với Permission
 * Xác định menu nào có những permission nào
 */
@Entity
@Table(name = "menu_permission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuPermission extends BaseEntity {

    @Column(name = "menu_id", nullable = false)
    private String menuId;

    @Column(name = "permission_id", nullable = false)
    private String permissionId;
}
