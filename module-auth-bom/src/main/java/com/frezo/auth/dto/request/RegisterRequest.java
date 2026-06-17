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

    // Quyền truy cập dữ liệu: 1=Nội bộ, 2=Cấp cha con, 3=Toàn quyền
    @NotNull(message = "dataAction không được để trống")
    @JsonProperty("dataAction")
    private Short dataAction;

    // Person ID để link User với Person có sẵn
    @JsonProperty("personId")
    private String personId;

    // Role ID để assign role cho user (single, backward compatible)
    @JsonProperty("roleId")
    private String roleId;

    // Multiple role codes để assign nhiều role cho user
    @JsonProperty("roleIds")
    private List<String> roleIds;

    // Organization ID để link Person với Organization
    @JsonProperty("orgId")
    private String orgId;

    @JsonProperty("departmentId")
    private String departmentId;
}
