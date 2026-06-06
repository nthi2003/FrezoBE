package com.frezo.qtht.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@AllArgsConstructor

public enum OrganizationType {
    COMPANY("COMPANY", "Công ty"),
    DEPARTMENT("DEPARTMENT", "Phòng ban"),
    BRANCH("BRANCH", "Chi nhánh"),
    AGENCY("AGENCY", "Đại lý"),
    PARTNER("PARTNER", "Đối tác"),
    CUSTOMER("CUSTOMER", "Khách hàng"),
    SUPPLIER("SUPPLIER", "Nhà cung cấp"),
    GOVERNMENT("GOVERNMENT", "Cơ quan nhà nước"),
    EDUCATIONAL("EDUCATIONAL", "Tổ chức giáo dục"),
    HOSPITAL("HOSPITAL", "Bệnh viện"),
    OTHER("OTHER", "Khác");

    private final String code;
    private final String description;
}
