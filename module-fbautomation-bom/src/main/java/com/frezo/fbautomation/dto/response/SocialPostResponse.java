package com.frezo.fbautomation.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
public class SocialPostResponse {
    private String id;
    private String orgId;
    private String channel;
    private String targetId;
    private String targetName;
    private String title;
    private String content;
    private String mediaUrls;
    private String linkUrl;
    private OffsetDateTime scheduledAt;
    private OffsetDateTime publishedAt;
    private String status;
    private String externalId;
    private String externalUrl;
    private String errorMessage;
    private Integer retryCount;
    private String authorUsername;
    // Audit fields — kế thừa BaseEntity dùng LocalDateTime (không phải OffsetDateTime).
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
