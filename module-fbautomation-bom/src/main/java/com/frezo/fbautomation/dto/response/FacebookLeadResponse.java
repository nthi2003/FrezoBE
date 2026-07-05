package com.frezo.fbautomation.dto.response;

import lombok.Data;

@Data
public class FacebookLeadResponse {
    private String id;
    private String name;
    private String phone;
    private String email;
    private String address;
    private String sourceGroupId;
    private String sourceGroupName;
    private String profileUrl;
    private String status;
    private String importedCustomerId;
    private String note;
    private String createdDate;
}
