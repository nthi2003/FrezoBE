package com.frezo.qtbv.service;

import com.frezo.qtbv.dto.request.ArticleManagerEditRequest;

public interface ArticleVisionService {
    void managerEdit(String articleId, ArticleManagerEditRequest request);
}
