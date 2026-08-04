package com.frezo.fbautomation.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
public class ModeratedCommentResponse {
    private String id;
    private String platform;
    private String authorName;
    private String content;
    private String postUrl;
    private String status;
    private String matchedRuleId;
    private String matchedRuleName;
    private String replyText;
    private OffsetDateTime commentedAt;
    private String note;
    private LocalDateTime createdDate;
}
