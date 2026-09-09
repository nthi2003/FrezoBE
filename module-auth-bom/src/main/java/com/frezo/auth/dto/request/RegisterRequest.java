package com.frezo.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Size(min = 4, max = 50)
    private String username;

    @NotBlank
    @Size(min = 6, max = 50)
    private String password;

    private String email;

    private String fullname;

    @NotNull(message = "dataAction không được để trống")
    @JsonProperty("dataAction")
    private Short dataAction;

    @JsonProperty("personId")
    private String personId;

    @JsonProperty("roleIds")
    private List<String> roleIds;

    @JsonProperty("orgId")
    private String orgId;

    @JsonProperty("departmentId")
    private String departmentId;
}
