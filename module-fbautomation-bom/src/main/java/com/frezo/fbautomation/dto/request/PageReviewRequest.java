package com.frezo.fbautomation.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class PageReviewRequest {
    @Size(max = 32) private String platform;

    @NotNull(message = "Rating bắt buộc")
    @Min(1) @Max(5)
    private Integer rating;

    @Size(max = 255) private String authorName;
    private String content;
    @Size(max = 16) private String status;
    private String replyText;
    private OffsetDateTime reviewedAt;
    @Size(max = 1000) private String externalUrl;
    private String note;
}
