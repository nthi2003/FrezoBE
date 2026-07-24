package com.frezo.qtbv.service;

import com.frezo.common.response.PageResponse;
import com.frezo.qtbv.dto.response.ArticleResponse;
import com.frezo.qtbv.entity.LandingConfig;
import com.frezo.product.dto.response.ProductResponse;
import com.frezo.product.dto.request.ProductFilterRequest;

public interface PublicService {
    LandingConfig getLandingConfig();
    PageResponse<ProductResponse> getProducts(ProductFilterRequest filterRequest);
    ProductResponse getProductDetail(String id);
    PageResponse<ArticleResponse> getArticles(int page, int size);
    Object getArticleDetail(String id);
}
