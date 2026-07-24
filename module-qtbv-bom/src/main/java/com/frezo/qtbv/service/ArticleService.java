package com.frezo.qtbv.service;

import com.frezo.qtbv.dto.request.ArticleCreateRequest;
import com.frezo.qtbv.dto.request.ArticleFilterRequest;
import com.frezo.qtbv.dto.request.ArticleReviewRequest;
import com.frezo.qtbv.dto.request.ArticleUpdateRequest;
import com.frezo.common.response.PageResponse;
import com.frezo.qtbv.dto.response.ArticleResponse;

import java.util.List;
import java.util.Map;

public interface ArticleService {
    ArticleResponse create(ArticleCreateRequest request);

    ArticleResponse update(String id, ArticleUpdateRequest request, String authorId);

    void submitForApproval(String id, String authorId);

    void delete(String id, String managerId);

    ArticleResponse publish(String id, String managerId);

    List<ArticleResponse> getPublishedArticles(String organizationId);

    /** Home portal feed for any authenticated user (INTERNAL + PUBLIC published). */
    List<ArticleResponse> getHomeFeed();

    /** Published article detail for Home /bai-viet reader (no QTBV admin permission). */
    ArticleResponse getHomeFeedById(String id);

    ArticleResponse review(String id, ArticleReviewRequest request, String managerId);

    ArticleResponse findById(String id);

    PageResponse<ArticleResponse> getPublicArticles(Integer pageNumber, Integer pageSize);

    Map<String, Object> filter(ArticleFilterRequest request);

    PageResponse<ArticleResponse> getMyDrafts(Integer pageNumber, Integer pageSize, String authorId);

    PageResponse<ArticleResponse> getPendingApproval(Integer pageNumber, Integer pageSize, String managerId);

}
