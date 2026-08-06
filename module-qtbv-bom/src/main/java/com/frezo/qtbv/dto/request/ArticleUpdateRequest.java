package com.frezo.qtbv.dto.request;

import com.frezo.qtbv.common.ArticleContentType;
import com.frezo.qtbv.common.PublishScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @Size(max = 100, message = "Tiêu đề tối đa 100 ký tự")
    private String title;

    @NotBlank(message = "Nội dung bài viết không được để trống")
    private String content;

    private String summary;
    private String thumbnailUrl;
    private String type;
    private String categoryId;
    private ArticleContentType contentType;
    private String externalUrl;
    private Boolean displayOnNews;

    private String organizationId;

    private PublishScope publishScope = PublishScope.INTERNAL;
    private Boolean isPublic;

    /** Người duyệt — có thể đổi khi bài còn DRAFT / REJECTED. */
    private String managerId;
}
