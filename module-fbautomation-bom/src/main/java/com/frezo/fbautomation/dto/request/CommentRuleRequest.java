package com.frezo.fbautomation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CommentRuleRequest {
    @NotBlank(message = "Tên rule bắt buộc")
    @Size(max = 255)
    private String name;

    @NotBlank(message = "Từ khoá bắt buộc")
    private String keywords;

    @Size(max = 16) private String action;
    private String replyTemplate;
    private Boolean enabled;
    private String note;
}
