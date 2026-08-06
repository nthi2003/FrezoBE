package com.frezo.qtht.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonResponse {
    private String id;

    private String code;

    private String name;

    private String shortName;

    private Boolean activated;

    private LocalDate dob;

    @JsonProperty("birthDate")
    public LocalDate getBirthDate() {
        return dob;
    }

    private String phone;

    private String jobTitle;

    private Boolean isAdmin = false;

    private String address;

    private String email;

    private String gender;

    private String description;

    private String orgId;

    private String orgName;

    private String departmentId;

    private String departmentName;

    private String avatarUrl;

    private String identityNumber;

    private String socialInsuranceNumber;

    private String bankAccount;

    private String bankName;

    private String bankBranch;

    private LocalDate joinDate;

    private LocalDate resignDate;

    private String jobPositionId;

    private String idCardFrontUrl;

    private String idCardBackUrl;
}
