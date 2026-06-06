package com.frezo.qtht.dto.response;

import com.frezo.qtht.common.OrganizationScale;
import com.frezo.qtht.common.OrganizationStatus;
import com.frezo.qtht.common.OrganizationType;
import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizationDetailResponse {
    // Thông tin cơ bản
    private String id;
    private String code;
    private String taxCode;
    private String name;
    private String shortName;
    private String nameEn;
    private String description;

    // Thông tin liên hệ chính
    private String website;
    private String email;
    private String phone;
    private String fax;
    private String address;
    private String logoUrl;
    private String provinceCode;
    private String wardCode;

    // Phân cấp tổ chức
    private String parentId;
    private OrganizationParentInfo parent;
    private Integer level;
    private Integer orderIndex;
    private String path;

    // Phân loại & Quy mô
    private OrganizationType type;
    private OrganizationScale scale;
    private String businessSector;
    private LocalDateTime establishedDate;

    // Trạng thái
    private OrganizationStatus status;

    // Thông tin người liên hệ
    private String contactPerson;
    private String contactPosition;
    private String contactEmail;
    private String contactPhone;

    // Người đại diện pháp luật
    private String legalRepresentativeId;

    // Thông tin hệ thống
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    /**
     * Thông tin tổ chức cha (nested)
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrganizationParentInfo {
        private String id;
        private String code;
        private String name;
        private String shortName;
    }
}
