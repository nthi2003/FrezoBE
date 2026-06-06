package com.frezo.qtht.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PersonUpdateRequest {

    private String code;
    private LocalDate dob;
    private String address;
    private String description;

    @JsonProperty("isAdmin")
    private Boolean isAdmin;

    @JsonProperty("activated")
    private Boolean activated;
    private String email;
    private String jobTitle;
    private String gender;
    private String orgId;
    private String departmentId;
    private String avatarUrl;
}
