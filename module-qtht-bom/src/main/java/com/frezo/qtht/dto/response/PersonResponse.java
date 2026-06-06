package com.frezo.qtht.dto.response;

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

    private String phone;

    private String jobTitle;

    private Boolean isAdmin = false;

    private String address;

    private String email;

    private String gender;

    private String description;

    private String orgId;

    private String orgName;

    private String avatarUrl;
}
