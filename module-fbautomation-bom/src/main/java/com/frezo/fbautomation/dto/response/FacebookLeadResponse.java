package com.frezo.fbautomation.dto.response;

import lombok.Data;

@Data
public class FacebookLeadResponse {
    private String id;
    private String name;
    private String phone;
    private String email;
    private String address;

    // ---- FB group crawler ----
    private String sourceGroupId;
    private String sourceGroupName;
    private String profileUrl;

    // ---- Trạng thái ----
    /** NEW | IMPORTED | ASSIGNED | REPLIED | CLOSED */
    private String status;
    private String importedCustomerId;
    private String note;

    // ---- Multi-channel inbox ----
    /** FACEBOOK | LANDING | ZALO | MANUAL — Default FACEBOOK cho data cũ. */
    private String source;
    private String subject;
    private String message;
    private String sourceIp;
    private String referer;
    private String assignedTo;

    private String createdDate;
}
