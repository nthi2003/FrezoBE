package com.frezo.qtht.dto.request;

import com.frezo.qtht.common.DepartmentStatus;
import lombok.Data;

@Data
public class DepartmentSaveRequest {
    private String name;
    private String code;
    private String shortName;
    private String description;
    private String parentId;
    private String organizationId;
    private String email;
    private String phone;
    private String fax;
    private String address;
    private String managerId;
    private String deputyManagerId;
    private Integer orderIndex;
    private DepartmentStatus status;
}
