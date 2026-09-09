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

    /** Phân lọc danh sách sản phẩm theo từ khóa, danh mục và trạng thái. */
    @Override
    public PageResponse<ProductResponse> filter(ProductFilterRequest filterRequest) {
        return commandService.filter(filterRequest);
    }

    /** Lấy chi tiết sản phẩm theo ID và tự động tăng lượt xem. */
    @Override
    public ProductResponse getById(String id) {
        return commandService.getById(id);
    }

    /** Tạo mới sản phẩm và ghi nhận lịch sử giá bán lẻ ban đầu. */
    @Override
    public ProductResponse create(ProductCreateRequest request) {
        return commandService.create(request);
    }

    /** Cập nhật thông tin sản phẩm, kiểm tra trùng mã và ghi nhận thay đổi giá. */
    @Override
    public ProductResponse update(String id, ProductUpdateRequest request) {
        return commandService.update(id, request);
    }

    /** Xóa mềm sản phẩm bằng cách đánh dấu isDeleted = true. */
    @Override
    public void delete(String id) {
        commandService.delete(id);
    }

    /** Cập nhật hàng loạt giá bán theo đơn vị và nhóm giá. */
    @Override
    public void bulkUpdatePrices(List<PriceUpdateRequest> requests) {
        pricingService.bulkUpdatePrices(requests);
    }

    /** Nhập kho nhanh theo chuyến xe — tạo lô hàng và ghi nhật ký tồn kho. */
    @Override
    public void importBatch(BatchImportRequest request) {
        inventoryService.importBatch(request);
    }

    /** Nhập sản phẩm hàng loạt từ file Excel (.xlsx/.xls) với cơ chế upsert. */
    @Override
    public ProductImportResultResponse importFromExcel(MultipartFile file) {
        return excelImportService.importFromExcel(file);
    }

    /** Tính giá bán theo mã sản phẩm, đơn vị và nhóm giá. */
    @Override
    public Double calculatePrice(String productCode, String unitName, String priceGroupCode) {
        return pricingService.calculatePrice(productCode, unitName, priceGroupCode);
    }

    /** Lấy thống kê tổng quan cho dashboard sản phẩm (tổng SP, sắp hết hàng, sắp hết hạn). */
    @Override
    public ProductDashboardStats getDashboardStats() {
        return dashboardService.getDashboardStats();
    }

    /** Lấy dữ liệu biểu đồ lợi nhuận theo số ngày gần đây. */
    @Override
    public List<Map<String, Object>> getProfitChart(int days) {
        return dashboardService.getProfitChart(days);
    }

    /** Lấy dữ liệu biến động giá bán so với ngày hôm trước. */
    @Override
    public List<Map<String, Object>> getPriceFluctuation() {
        return dashboardService.getPriceFluctuation();
    }

    /** Lấy dữ liệu so sánh giá nội bộ với giá thị trường. */
    @Override
    public List<Map<String, Object>> getMarketComparison() {
        return dashboardService.getMarketComparison();
    }

    /** Lấy lịch sử giá vốn nhập kho theo sản phẩm. */
    @Override
    public List<Map<String, Object>> getCostHistory(String productId) {
        return inventoryService.getCostHistory(productId);
    }

    /** Cập nhật giá bán lẻ của sản phẩm và ghi nhận lịch sử thay đổi giá. */
    @Override
    public ProductResponse updateSalePrice(String productId, com.frezo.product.dto.request.ProductSalePriceUpdateRequest request) {
        return pricingService.updateSalePrice(productId, request);
    }

    /** Lấy lịch sử thay đổi giá bán lẻ theo sản phẩm. */
    @Override
    public List<ProductSalePriceHistoryResponse> getSalePriceHistory(String productId) {
        return pricingService.getSalePriceHistory(productId);
    }

    /** Upload hình ảnh sản phẩm lên MinIO và trả về URL. */
    @Override
    public Map<String, Object> uploadImage(MultipartFile file) {
        return imageService.uploadImage(file);
    }
}
