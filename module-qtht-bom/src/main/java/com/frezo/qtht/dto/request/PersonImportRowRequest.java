package com.frezo.qtht.dto.request;

import lombok.Data;

@Data
public class PersonImportRowRequest {
    private String code;
    private String name;
    private String email;
    private String phone;
    private String gender;
    private String birthDate;
    private String identityNumber;
    private String address;
    private String departmentId;
    private String orgId;
    private String jobTitle;
    private String socialInsuranceNumber;
    private String bankAccount;
    private String bankName;
    private String joinDate;
}
