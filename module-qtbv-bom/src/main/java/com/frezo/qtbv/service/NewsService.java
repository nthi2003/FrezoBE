package com.frezo.qtbv.service;

import com.frezo.qtbv.dto.request.ArticlePinRequest;
import com.frezo.qtbv.dto.request.NewsCategoryRequest;
import com.frezo.qtbv.dto.request.NewsMottoRequest;
import com.frezo.qtbv.dto.response.*;

import java.util.List;

public interface NewsService {

    List<NewsCategoryResponse> listCategories(String organizationId);

    NewsCategoryResponse createCategory(NewsCategoryRequest request);

    NewsCategoryResponse updateCategory(String id, NewsCategoryRequest request);

    void deleteCategory(String id);

    List<NewsMottoResponse> listMottos();

    NewsMottoResponse createMotto(NewsMottoRequest request);

    NewsMottoResponse updateMotto(String id, NewsMottoRequest request);

    void deleteMotto(String id);

    List<ArticleResponse> listPins(String organizationId);

    ArticleResponse pinArticle(ArticlePinRequest request);

    void unpinArticle(String organizationId, String articleId);

    NewsPageDataResponse getNewsPageData(String organizationId);
}
