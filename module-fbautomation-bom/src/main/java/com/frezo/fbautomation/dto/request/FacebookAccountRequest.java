package com.frezo.fbautomation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FacebookAccountRequest {

    @NotBlank(message = "Username không được để trống")
    private String username;

    @NotBlank(message = "Password không được để trống")
    private String password;

    private String cookie;
    private String proxyIp;
    private String status;
    private String userAgent;
}
