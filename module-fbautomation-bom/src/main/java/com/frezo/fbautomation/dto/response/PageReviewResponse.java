package com.frezo.fbautomation.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Data
public class PageReviewResponse {
    private String id;
    private String platform;
    private Integer rating;
    private String authorName;
    private String content;
    private String status;
    private String replyText;
    private OffsetDateTime reviewedAt;
    private String externalUrl;
    private String note;
    private Boolean lowRating;
    private LocalDateTime createdDate;
}
