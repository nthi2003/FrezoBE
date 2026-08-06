package com.frezo.qtht.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PersonUpdateRequest {

    private String code;
    private String name;
    private LocalDate dob;

    @JsonProperty("birthDate")
    public LocalDate getBirthDate() {
        return dob;
    }

    @JsonProperty("birthDate")
    public void setBirthDate(LocalDate birthDate) {
        this.dob = birthDate;
    }

    private String address;
    private String description;

    @JsonProperty("isAdmin")
    private Boolean isAdmin;

    @NotNull(message = "Trạng thái không được để trống")
    @JsonProperty("activated")
    private Boolean activated;
    private String email;
    private String phone;
    private String jobTitle;
    private String gender;
    private String orgId;
    private String departmentId;
    private String avatarUrl;

    @JsonProperty("identityNumber")
    private String identityNumber;

    @JsonProperty("socialInsuranceNumber")
    private String socialInsuranceNumber;

    @JsonProperty("bankAccount")
    private String bankAccount;

    @JsonProperty("bankName")
    private String bankName;

    @JsonProperty("bankBranch")
    private String bankBranch;

    @JsonProperty("joinDate")
    private LocalDate joinDate;

    @JsonProperty("resignDate")
    private LocalDate resignDate;

    @JsonProperty("jobPositionId")
    private String jobPositionId;

    @JsonProperty("idCardFrontUrl")
    private String idCardFrontUrl;

    @JsonProperty("idCardBackUrl")
    private String idCardBackUrl;
}
