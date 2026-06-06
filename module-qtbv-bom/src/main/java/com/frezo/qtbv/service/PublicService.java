package com.frezo.qtbv.service;

import com.frezo.common.response.PageResponse;
import com.frezo.qtbv.entity.LandingConfig;
import com.frezo.product.dto.response.ProductResponse;
import com.frezo.product.dto.request.ProductFilterRequest;

import java.util.List;
import java.util.Map;

public interface PublicService {
    LandingConfig getLandingConfig();
    PageResponse<ProductResponse> getProducts(ProductFilterRequest filterRequest);
    ProductResponse getProductDetail(String id);
    Map<String, Object> getArticles(int page, int size);
    Object getArticleDetail(String id);
}
