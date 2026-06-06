package com.frezo.qtbv.dto.request;

import com.frezo.qtbv.common.PublishScope;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ArticleUpdateRequest {
    @NotBlank(message = "Tiêu đề bài viết không được để trống")
    private String title;

    @NotBlank(message = "Nội dung bài viết không được để trống")
    private String content;

    private String organizationId;

    private PublishScope publishScope = PublishScope.INTERNAL;
    private Boolean isPublic;
}
