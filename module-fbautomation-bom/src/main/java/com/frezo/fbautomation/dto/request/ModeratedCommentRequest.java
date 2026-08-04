package com.frezo.fbautomation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ModeratedCommentRequest {
    @Size(max = 32) private String platform;

    @Size(max = 255) private String authorName;

    @NotBlank(message = "Nội dung comment bắt buộc")
    private String content;

    @Size(max = 1000) private String postUrl;
    @Size(max = 16) private String status;
    private String replyText;
    private OffsetDateTime commentedAt;
    private String note;
}
