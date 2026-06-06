package com.frezo.qtht.entity;

import com.frezo.common.domain.BaseEntity;
import com.frezo.qtht.common.DepartmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "department")
public class Department extends BaseEntity {

    @Column(name = "code", length = 50, unique = true, nullable = false)
    private String code; // Mã phòng ban (ví dụ: IT, HR, SALES)

    @Column(name = "name", length = 255, nullable = false)
    private String name; // Tên phòng ban

    @Column(name = "short_name", length = 100)
    private String shortName; // Tên viết tắt

    @Column(name = "name_en", length = 255)
    private String nameEn; // Tên tiếng Anh

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Mô tả

    @Column(name = "email", length = 100)
    private String email; // Email phòng ban

    @Column(name = "phone", length = 20)
    private String phone; // Số điện thoại

    @Column(name = "fax", length = 20)
    private String fax; // Số fax

    @Column(name = "address", length = 500)
    private String address; // Địa chỉ


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", referencedColumnName = "id")
    private Organization organization;

    @Column(name = "organization_id", insertable = false, updatable = false)
    private String organizationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", referencedColumnName = "id")
    private Department parent;

    @Column(name = "parent_id", insertable = false, updatable = false)
    private String parentId;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Department> children = new ArrayList<>();

    @Column(name = "level", nullable = false)
    private Integer level ;

    @Column(name = "order_index")
    private Integer orderIndex ; // Thứ tự sắp xếp

    @Column(name = "path", length = 1000)
    private String path; // Đường dẫn phân cấp (ví dụ: /1/2/3/)


    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private DepartmentStatus status = DepartmentStatus.ACTIVE;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", referencedColumnName = "id")
    private Person manager;

    @Column(name = "manager_id", insertable = false, updatable = false)
    private String managerId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "deputy_manager_id", referencedColumnName = "id")
    private Person deputyManager;

    @Column(name = "deputy_manager_id", insertable = false, updatable = false)
    private String deputyManagerId;
}