package com.frezo.qtbv.dto.request;

import com.frezo.qtbv.common.ArticleContentType;
import com.frezo.qtbv.common.PublishScope;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Create article contract (SA-ART-001 / CYCLE-QTLV-ART).
 * <ul>
 *   <li>{@code code} — optional; blank/null → server auto-gen {@code QTBV-YYYYMMDD-###}</li>
 *   <li>{@code title}, {@code content} — required</li>
 *   <li>{@code organizationId}, {@code managerId}, {@code publishScope}, {@code isPublic} — optional</li>
 * </ul>
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ArticleCreateRequest {
    /** Optional. When blank/null, BE generates {@code QTBV-YYYYMMDD-###}. */
    @JsonProperty("code")
    private String code;

    @NotBlank(message = "Tiêu đề bài viết không được để trống")
    @Size(max = 100, message = "Tiêu đề tối đa 100 ký tự")
    @JsonProperty("title")
    private String title;

    @NotBlank(message = "Nội dung bài viết không được để trống")
    @JsonProperty("content")
    private String content;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("thumbnailUrl")
    private String thumbnailUrl;

    @JsonProperty("type")
    private String type;

    @JsonProperty("categoryId")
    private String categoryId;

    @JsonProperty("contentType")
    private ArticleContentType contentType;

    @JsonProperty("externalUrl")
    private String externalUrl;

    @JsonProperty("displayOnNews")
    private Boolean displayOnNews;

    @JsonProperty("organizationId")
    private String organizationId;

    @JsonProperty("publishScope")
    private PublishScope publishScope = PublishScope.INTERNAL;

    @JsonProperty("managerId")
    private String managerId;

    private Boolean isPublic;
}
