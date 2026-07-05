package com.frezo.fbautomation.dto.request;

import lombok.Data;

@Data
public class FacebookGroupRequest {

    private String groupId;
    private String groupName;
    private Integer memberCount;
    private Double relevanceScore;
    private String status;
    private String category;
    private String description;
    private String groupUrl;
}
