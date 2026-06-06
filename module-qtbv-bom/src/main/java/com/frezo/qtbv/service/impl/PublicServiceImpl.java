package com.frezo.qtbv.service.impl;

import com.frezo.common.response.PageResponse;
import com.frezo.qtbv.entity.LandingConfig;
import com.frezo.qtbv.service.ArticleService;
import com.frezo.qtbv.service.LandingConfigService;
import com.frezo.qtbv.service.PublicService;
import com.frezo.product.dto.request.ProductFilterRequest;
import com.frezo.product.dto.response.ProductResponse;
import com.frezo.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PublicServiceImpl implements PublicService {

    private final LandingConfigService landingConfigService;
    private final ProductService productService;
    private final ArticleService articleService;

    @Override
    public LandingConfig getLandingConfig() {
        return landingConfigService.getConfig();
    }

    @Override
    public PageResponse<ProductResponse> getProducts(ProductFilterRequest filterRequest) {
        if (filterRequest == null) filterRequest = new ProductFilterRequest();
        return productService.filter(filterRequest);
    }

    @Override
    public ProductResponse getProductDetail(String id) {
        return productService.getById(id);
    }

    @Override
    public Map<String, Object> getArticles(int page, int size) {
        return articleService.getPublicArticles(page, size);
    }

    @Override
    public Object getArticleDetail(String id) {
        return articleService.findById(id);
    }
}
