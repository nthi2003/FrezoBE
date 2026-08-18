package com.frezo.product.service.impl;

import com.frezo.common.response.PageResponse;
import com.frezo.product.dto.request.BatchImportRequest;
import com.frezo.product.dto.request.PriceUpdateRequest;
import com.frezo.product.dto.request.ProductCreateRequest;
import com.frezo.product.dto.request.ProductFilterRequest;
import com.frezo.product.dto.request.ProductUpdateRequest;
import com.frezo.product.dto.response.ProductDashboardStats;
import com.frezo.product.dto.response.ProductImportResultResponse;
import com.frezo.product.dto.response.ProductResponse;
import com.frezo.product.dto.response.ProductSalePriceHistoryResponse;
import com.frezo.product.service.ProductService;
import com.frezo.product.service.impl.product.ProductCommandService;
import com.frezo.product.service.impl.product.ProductDashboardService;
import com.frezo.product.service.impl.product.ProductExcelImportService;
import com.frezo.product.service.impl.product.ProductImageService;
import com.frezo.product.service.impl.product.ProductInventoryService;
import com.frezo.product.service.impl.product.ProductPricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Façade cho toàn bộ Product API.
 * <p>
 * Sau refactor Batch B (2026-07): tách theo concern:
 * <ul>
 *   <li>{@link ProductCommandService} — CRUD sản phẩm</li>
 *   <li>{@link ProductPricingService} — bulk update giá + tính giá theo unit/group</li>
 *   <li>{@link ProductInventoryService} — nhập kho theo chuyến (batch + inventory log)</li>
 *   <li>{@link ProductDashboardService} — KPI + biểu đồ dashboard</li>
 *   <li>{@link ProductImageService} — upload ảnh MinIO</li>
 *   <li>{@link ProductExcelImportService} — import catalog Excel</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductCommandService commandService;
    private final ProductPricingService pricingService;
    private final ProductInventoryService inventoryService;
    private final ProductDashboardService dashboardService;
    private final ProductImageService imageService;
    private final ProductExcelImportService excelImportService;

    @Override
    public PageResponse<ProductResponse> filter(ProductFilterRequest filterRequest) {
        return commandService.filter(filterRequest);
    }

    @Override
    public ProductResponse getById(String id) {
        return commandService.getById(id);
    }

    @Override
    public ProductResponse create(ProductCreateRequest request) {
        return commandService.create(request);
    }

    @Override
    public ProductResponse update(String id, ProductUpdateRequest request) {
        return commandService.update(id, request);
    }

    @Override
    public void delete(String id) {
        commandService.delete(id);
    }

    @Override
    public void bulkUpdatePrices(List<PriceUpdateRequest> requests) {
        pricingService.bulkUpdatePrices(requests);
    }

    @Override
    public void importBatch(BatchImportRequest request) {
        inventoryService.importBatch(request);
    }

    @Override
    public ProductImportResultResponse importFromExcel(MultipartFile file) {
        return excelImportService.importFromExcel(file);
    }

    @Override
    public Double calculatePrice(String productCode, String unitName, String priceGroupCode) {
        return pricingService.calculatePrice(productCode, unitName, priceGroupCode);
    }

    @Override
    public ProductDashboardStats getDashboardStats() {
        return dashboardService.getDashboardStats();
    }

    @Override
    public List<Map<String, Object>> getProfitChart(int days) {
        return dashboardService.getProfitChart(days);
    }

    @Override
    public List<Map<String, Object>> getPriceFluctuation() {
        return dashboardService.getPriceFluctuation();
    }

    @Override
    public List<Map<String, Object>> getMarketComparison() {
        return dashboardService.getMarketComparison();
    }

    @Override
    public List<Map<String, Object>> getCostHistory(String productId) {
        return inventoryService.getCostHistory(productId);
    }

    @Override
    public ProductResponse updateSalePrice(String productId, com.frezo.product.dto.request.ProductSalePriceUpdateRequest request) {
        return pricingService.updateSalePrice(productId, request);
    }

    @Override
    public List<ProductSalePriceHistoryResponse> getSalePriceHistory(String productId) {
        return pricingService.getSalePriceHistory(productId);
    }

    @Override
    public Map<String, Object> uploadImage(MultipartFile file) {
        return imageService.uploadImage(file);
    }
}
