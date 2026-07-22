package com.frezo.fbautomation.dto.request;

import lombok.Data;

@Data
public class FacebookLeadRequest {

    private String name;
    private String phone;
    private String email;
    private String address;
    private String sourceGroupId;
    private String sourceGroupName;
    private String profileUrl;
    private String status;
    private String note;

    // ---- Multi-channel inbox ----
    private String source;   // FACEBOOK | LANDING | ZALO | MANUAL
    private String subject;
    private String message;
    private String sourceIp;
    private String referer;
    private String assignedTo;
}
