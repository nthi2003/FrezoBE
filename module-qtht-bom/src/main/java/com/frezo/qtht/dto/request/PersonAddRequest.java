package com.frezo.qtht.dto.request;

import jakarta.persistence.Column;
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

    private String code;

    private String name;

    @JsonProperty("shortName")
    private String shortName;

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
