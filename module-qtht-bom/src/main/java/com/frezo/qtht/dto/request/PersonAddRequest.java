package com.frezo.qtht.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonProperty;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonAddRequest {

    private String id;

    @NotBlank(message = "Mã nhân viên không được để trống")
    private String code;

    @NotBlank(message = "Tên nhân viên không được để trống")
    private String name;

    @JsonProperty("shortName")
    private String shortName;

    @NotNull(message = "Trạng thái không được để trống")
    private Boolean activated;

    private LocalDate dob;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("jobTitle")
    private String jobTitle;

    @JsonProperty("isAdmin")
    private Boolean isAdmin = false;

    @JsonProperty("address")
    private String address;

    private String email;

    @JsonProperty("description")
    private String description;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("orgId")
    private String orgId;

    @JsonProperty("departmentId")
    private String departmentId;

    @JsonProperty("avatarUrl")
    private String avatarUrl;
}
