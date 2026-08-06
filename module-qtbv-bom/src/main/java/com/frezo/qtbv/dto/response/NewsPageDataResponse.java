package com.frezo.qtbv.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsPageDataResponse {
    private List<BannerResponse> banners;
    private NewsMottoResponse motto;
    private List<NewsCategoryResponse> categories;
    private List<ArticleResponse> pinnedArticles;
    private List<ArticleResponse> articles;
    /** Đơn vị đã resolve (từ param hoặc user hiện tại) — dùng cho ghim tin theo org. */
    private String resolvedOrganizationId;
}
