package com.frezo.qtht.dto.request;

import com.frezo.common.model.PagingBase;
import com.frezo.qtht.common.OrganizationScale;
import com.frezo.qtht.common.OrganizationStatus;
import com.frezo.qtht.common.OrganizationType;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;


@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrganizationFilterRequest extends PagingBase {

    private String keyword;

    private String code; // Mã tổ chức (ví dụ: FPT, VNPT, VPB)

    private String taxCode; // Mã số thuế


    private String name; // Tên tổ chức


    private String shortName; // Tên viết tắt


    private String nameEn; // Tên tiếng Anh

    private String description; // Mô tả


    private String logoUrl; // URL logo


    private String website; // Website


    private String email; // Email liên hệ


    private String phone; // Số điện thoại


    private String fax; // Số fax


    private String address; // Địa chỉ


    private String provinceCode; // Mã tỉnh/thành phố


    private String wardCode; // Mã phường/xã

    private String parentId;


    private Integer level ;

    private Integer orderIndex ; // Thứ tự sắp xếp

    private String path; // Đường dẫn phân cấp (ví dụ: /1/2/3/)


    private OrganizationType type ;

    private OrganizationScale scale;

    private String businessSector;


    private LocalDateTime establishedDate;

    private OrganizationStatus status ;



    private String legalRepresentative;


    private String legalRepresentativeId;


    private String contactPerson;


    private String contactPosition;


    private String contactEmail;


    private String contactPhone;
}
