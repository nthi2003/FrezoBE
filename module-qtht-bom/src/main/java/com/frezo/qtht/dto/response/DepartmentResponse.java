package com.frezo.qtht.dto.response;

import com.frezo.qtht.common.DepartmentStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DepartmentResponse {

    private String id;
    private String code; // Mã phòng ban (ví dụ: IT, HR, SALES)
    private String name; // Tên phòng ban
    private String shortName; // Tên viết tắt
    private String nameEn; // Tên tiếng Anh
    private String description; // Mô tả
    private String email; // Email phòng ban
    private String phone; // Số điện thoại
    private String fax; // Số fax
    private String address; // Địa chỉ

    private String organizationId;
    private String organizationName;

    private String parentId;
    private String parentName;

    private Integer level;
    private Integer orderIndex; // Thứ tự sắp xếp
    private String path; // Đường dẫn phân cấp (ví dụ: /1/2/3/)

    private DepartmentStatus status;

    private String managerId;
    private String managerName;

    private String deputyManagerId;
    private String deputyManagerName;
}
