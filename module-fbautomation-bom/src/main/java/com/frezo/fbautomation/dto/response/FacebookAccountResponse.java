package com.frezo.fbautomation.dto.response;

import lombok.Data;

@Data
public class FacebookAccountResponse {
    private String id;
    private String username;
    private String proxyIp;
    private String status;
    private String userAgent;
    private Integer postsToday;
    private String createdDate;
}
