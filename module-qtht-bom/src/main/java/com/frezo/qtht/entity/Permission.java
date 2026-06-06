package com.frezo.qtht.entity;

import com.frezo.common.domain.BaseEntity;
import lombok.*;
import jakarta.persistence.*;

/**
 * Permission entity - Quản lý các quyền hạn chi tiết trong hệ thống
 * Ví dụ: VIEW_MENU, CREATE_MENU, UPDATE_MENU, DELETE_MENU
 */
@Entity
@Table(name = "permission")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission extends BaseEntity {

    @Column(name = "code", length = 50, nullable = false, unique = true)
    private String code; // MENU_VIEW, MENU_CREATE, ROLE_UPDATE...

    @Column(name = "name", length = 200, nullable = false)
    private String name; // Xem danh sách menu

    @Column(name = "name_en", length = 200)
    private String nameEn; // View menu list

    @Column(name = "api_method", length = 10)
    private String apiMethod; // GET, POST, PUT, DELETE

    @Column(name = "api_path", length = 500)
    private String apiPath; // /qlht/menus

    @Column(name = "action", length = 50)
    private String action; // VIEW, CREATE, UPDATE, DELETE

    @Column(name = "app_code", length = 50)
    private String appCode; // QTHT

    @Column(name = "description", length = 500)
    private String description;
}
