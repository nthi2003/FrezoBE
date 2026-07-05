package com.frezo.fbautomation.dto.response;

import lombok.Data;

@Data
public class FacebookGroupResponse {
    private String id;
    private String groupId;
    private String groupName;
    private Integer memberCount;
    private Double relevanceScore;
    private String status;
    private String category;
    private String description;
    private String groupUrl;
}
