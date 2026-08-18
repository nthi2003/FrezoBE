package com.frezo.product.service;

import com.frezo.common.response.PageResponse;
import com.frezo.product.dto.request.*;
import com.frezo.product.dto.response.ProductDashboardStats;
import com.frezo.product.dto.response.ProductImportResultResponse;
import com.frezo.product.dto.response.ProductResponse;
import com.frezo.product.dto.response.ProductSalePriceHistoryResponse;
import org.springframework.web.multipart.MultipartFile;

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
    /** Import catalog từ Excel (.xlsx/.xls); upsert theo mã; bỏ qua cột %. */
    ProductImportResultResponse importFromExcel(MultipartFile file);
    Double calculatePrice(String productCode, String unitName, String priceGroupCode);
    ProductDashboardStats getDashboardStats();
    List<Map<String, Object>> getProfitChart(int days);
    List<Map<String, Object>> getPriceFluctuation();
    List<Map<String, Object>> getMarketComparison();
    /** Lịch sử giá vốn theo lô nhập (product_batches.cost_price). */
    List<Map<String, Object>> getCostHistory(String productId);

    ProductResponse updateSalePrice(String productId, ProductSalePriceUpdateRequest request);

    List<ProductSalePriceHistoryResponse> getSalePriceHistory(String productId);

    Map<String, Object> uploadImage(MultipartFile file);
}
