package com.frezo.qtht.entity;

import com.frezo.common.domain.BaseEntity;
import com.frezo.qtht.common.OrganizationScale;
import com.frezo.qtht.common.OrganizationStatus;
import com.frezo.qtht.common.OrganizationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "organization")
public class Organization extends BaseEntity {
    @Column(name = "code", length = 50, unique = true, nullable = false)
    private String code; // Mã tổ chức (ví dụ: FREZO)

    @Column(name = "tax_code", length = 20, unique = true)
    private String taxCode; // Mã số thuế

    @Column(name = "name", length = 255, nullable = false)
    private String name; // Tên đầy đủ

    @Column(name = "short_name", length = 100)
    private String shortName; // Tên viết tắt

    @Column(name = "name_en", length = 255)
    private String nameEn; // Tên tiếng Anh

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // Mô tả

    @Column(name = "logo_url", length = 500)
    private String logoUrl; // URL logo

    @Column(name = "website", length = 255)
    private String website; // Website

    @Column(name = "email", length = 100)
    private String email; // Email liên hệ

    @Column(name = "phone", length = 20)
    private String phone; // Số điện thoại

    @Column(name = "fax", length = 20)
    private String fax; // Số fax

    @Column(name = "address", length = 500)
    private String address; // Địa chỉ trụ sở

    @Column(name = "province_code", length = 20)
    private String provinceCode; // Mã tỉnh/thành phố

    @Column(name = "ward_code", length = 20)
    private String wardCode; // Mã quận/huyện

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", referencedColumnName = "id")
    private Organization parent;

    @Column(name = "parent_id", insertable = false, updatable = false)
    private String parentId;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Organization> children = new ArrayList<>();

    @Column(name = "level", nullable = false)
    private Integer level ;

    @Column(name = "order_index")
    private Integer orderIndex ; // Thứ tự sắp xếp

    @Column(name = "path", length = 1000)
    private String path; // Đường dẫn phân cấp (ví dụ: /1/2/3/)

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50)
    private OrganizationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "scale", length = 50)
    private OrganizationScale scale;

    @Column(name = "business_sector", length = 255)
    private String businessSector; // Lĩnh vực kinh doanh

    @Column(name = "established_date")
    private LocalDateTime establishedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private OrganizationStatus status ;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "legal_representative_id", referencedColumnName = "id")
    private Person legalRepresentative;

    @Column(name = "legal_representative_id", insertable = false, updatable = false)
    private String legalRepresentativeId;

    @Column(name = "contact_person", length = 100)
    private String contactPerson;

    @Column(name = "contact_position", length = 100)
    private String contactPosition;

    @Column(name = "contact_email", length = 100)
    private String contactEmail;

    @Column(name = "contact_phone", length = 20)
    private String contactPhone;
}
