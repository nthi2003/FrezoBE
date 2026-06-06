package com.frezo.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank
    @Size(min = 4, max = 50)
    private String username;

    @NotBlank
    @Size(min = 6, max = 50)
    private String password;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 2, max = 100)
    private String fullname;

    // Quyền truy cập dữ liệu: 1=Nội bộ, 2=Cấp cha con, 3=Toàn quyền
    @NotNull(message = "dataAction không được để trống")
    @JsonProperty("dataAction") // Đảm bảo nhận đúng field name từ frontend (camelCase)
    private Short dataAction;

    // Role ID để assign role cho user
    @JsonProperty("roleId")
    private String roleId;

    // Organization ID để link Person với Organization
    @JsonProperty("orgId")
    private String orgId;
}
