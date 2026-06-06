package com.frezo.product.service;

import com.frezo.common.response.PageResponse;
import com.frezo.product.dto.request.*;
import com.frezo.product.dto.response.ProductDashboardStats;
import com.frezo.product.dto.response.ProductResponse;

import java.util.List;
import java.util.Map;

public interface ProductService {
    PageResponse<ProductResponse> filter(ProductFilterRequest filterRequest);
    ProductResponse getById(String id);
    ProductResponse create(ProductCreateRequest request);
    ProductResponse update(String id, ProductUpdateRequest request);
    void delete(String id);

    void bulkUpdatePrices(List<PriceUpdateRequest> requests);
    void importBatch(BatchImportRequest request);
    Double calculatePrice(String productCode, String unitName, String priceGroupCode);
    ProductDashboardStats getDashboardStats();
    List<Map<String, Object>> getProfitChart(int days);
    List<Map<String, Object>> getPriceFluctuation();
    List<Map<String, Object>> getMarketComparison();
}
