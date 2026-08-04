package com.frezo.fbautomation.service;

import com.frezo.fbautomation.dto.request.PageReviewRequest;
import com.frezo.fbautomation.dto.response.PageReviewResponse;

import java.util.List;
import java.util.Map;

public interface PageReviewService {
    List<PageReviewResponse> list(String status, String platform);
    PageReviewResponse get(String id);
    PageReviewResponse create(PageReviewRequest req);
    PageReviewResponse update(String id, PageReviewRequest req);
    void delete(String id);
    PageReviewResponse reply(String id, String replyText);
    Map<String, Object> dashboard();
}
