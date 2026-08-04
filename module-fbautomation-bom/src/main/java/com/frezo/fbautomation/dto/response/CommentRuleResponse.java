package com.frezo.fbautomation.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentRuleResponse {
    private String id;
    private String name;
    private String keywords;
    private String action;
    private String replyTemplate;
    private Boolean enabled;
    private Long hitCount;
    private String note;
    private LocalDateTime createdDate;
}
