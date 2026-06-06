package com.frezo.qtbv.service;

import com.frezo.qtbv.dto.request.ArticleCreateRequest;
import com.frezo.qtbv.dto.request.ArticleFilterRequest;
import com.frezo.qtbv.dto.request.ArticleReviewRequest;
import com.frezo.qtbv.dto.request.ArticleUpdateRequest;
import com.frezo.qtbv.dto.response.ArticleResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ArticleService {
    ArticleResponse create(ArticleCreateRequest request);

    ArticleResponse update(String id, ArticleUpdateRequest request, String authorId);

    void submitForApproval(String id, String authorId);

    void delete(String id, String managerId);

    ArticleResponse publish(String id, String managerId);

    List<ArticleResponse> getPublishedArticles(String organizationId);

    ArticleResponse review(String id, ArticleReviewRequest request, String managerId);

    ArticleResponse findById(String id);

    Map<String, Object> getPublicArticles(Integer pageNumber, Integer pageSize);

    Map<String, Object> filter(ArticleFilterRequest request);

    
    Map<String, Object> getMyDrafts(Integer pageNumber, Integer pageSize, String authorId);

  
    Map<String, Object> getPendingApproval(Integer pageNumber, Integer pageSize, String managerId);

}
