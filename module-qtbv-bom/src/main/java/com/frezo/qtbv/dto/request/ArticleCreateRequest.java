package com.frezo.qtbv.dto.request;

import com.frezo.qtbv.common.PublishScope;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ArticleCreateRequest {
    @NotBlank(message = "Mã bài viết không được để trống")
    @JsonProperty("code")
    private String code;

    @NotBlank(message = "Tiêu đề bài viết không được để trống")
    @JsonProperty("title")
    private String title;

    @NotBlank(message = "Nội dung bài viết không được để trống")
    @JsonProperty("content")
    private String content;

    @JsonProperty("organizationId")
    private String organizationId;

    @JsonProperty("publishScope")
    private PublishScope publishScope = PublishScope.INTERNAL;

    @JsonProperty("managerId")
    private String managerId;

    private Boolean isPublic;
}
