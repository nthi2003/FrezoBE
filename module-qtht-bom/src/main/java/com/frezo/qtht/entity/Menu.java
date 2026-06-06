package com.frezo.qtht.entity;

import com.frezo.common.domain.BaseEntity;
import lombok.*;

import jakarta.persistence.*;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "menu", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "app_code", "code" }) })
public class Menu extends BaseEntity {

    @Column(length = 50, nullable = false)
    private String code; // MENU_USER

    @Column(name = "app_code", length = 50, nullable = false)
    private String appCode; // QTHT
    private String name;
    private String nameEn;
    @Column(name = "parent_code")
    private String parentCode;
    @Column(name = "order_index")
    private Integer orderIndex;
    @Column(name = "menu_type")
    private Short menuType;
    @Column(name = "is_public")
    private Boolean isPublic;
    private String icon;
    @Column(name = "fe_url")
    private String feUrl;
    @Column(name = "folder_path")
    private String folderPath;

    @Column(name = "status")
    private Boolean status;

    // @OneToMany(mappedBy = "menu", cascade = CascadeType.ALL, fetch =
    // FetchType.LAZY)
    // private java.util.List<MenuPermission> menuPermissions;
}
