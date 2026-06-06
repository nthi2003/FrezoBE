package com.frezo.qtht.dto.response;

import com.frezo.qtht.common.OrganizationScale;
import com.frezo.qtht.common.OrganizationStatus;
import com.frezo.qtht.common.OrganizationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationResponse {
    private String id;
    private String code;
    private String taxCode;
    private String name;
    private String shortName;
    private String nameEn;
    private String description;
    private String logoUrl;
    private String website;
    private String email;
    private String phone;
    private String fax;
    private String address;
    private String provinceCode;
    private String wardCode;

    // Thông tin phân cấp
    private String parentId;
    private OrganizationParentResponse parent;
    private List<OrganizationChildResponse> children = new ArrayList<>();
    private Integer level;
    private Integer orderIndex;
    private String path;

    // Phân loại tổ chức
    private OrganizationType type;
    private OrganizationScale scale;
    private String businessSector;

    // Thời gian
    private LocalDateTime establishedDate;

    // Trạng thái
    private OrganizationStatus status;

    // Đại diện pháp luật
    private String legalRepresentativeId;
    private PersonResponse legalRepresentative;

    // Thông tin liên hệ
    private String contactPerson;
    private String contactPosition;
    private String contactEmail;
    private String contactPhone;

    // Thông tin thời gian từ BaseEntity
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    // DTO cho tổ chức cha (chỉ hiển thị thông tin cơ bản)
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizationParentResponse {
        private String id;
        private String code;
        private String name;
        private String shortName;
    }

    // DTO cho tổ chức con (chỉ hiển thị thông tin cơ bản)
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizationChildResponse {
        private String id;
        private String code;
        private String name;
        private String shortName;
        private Integer level;
        private Integer orderIndex;
        private OrganizationType type;
        private OrganizationStatus status;
    }

    // DTO cho người đại diện pháp luật
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PersonResponse {
        private String id;
        private String code;
        private String fullName;
        private String email;
        private String phone;
        private String position;
    }
}
