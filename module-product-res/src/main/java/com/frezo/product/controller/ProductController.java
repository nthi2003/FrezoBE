package com.frezo.product.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.product.dto.request.ProductCreateRequest;
import com.frezo.product.dto.request.ProductFilterRequest;
import com.frezo.product.dto.request.ProductUpdateRequest;
import com.frezo.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.frezo.qtht.config.CheckPermission;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
@Tag(name = "8. Quản lý sản phẩm", description = "API quản lý sản phẩm cho TheNewFarm")
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "Lọc danh sách sản phẩm", description = "Lọc danh sách sản phẩm theo tiêu chí")
    @PostMapping("/filter")
    @CheckPermission(api = "/product/filter", action = "VIEW")
    public ApiResponse<?> filter(@RequestBody(required = false) ProductFilterRequest filterRequest) {
        if (filterRequest == null) filterRequest = new ProductFilterRequest();
        return ApiResponse.success(productService.filter(filterRequest));
    }

    @Operation(summary = "Lấy chi tiết sản phẩm", description = "Lấy thông tin chi tiết của một sản phẩm")
    @GetMapping("/{id}")
    @CheckPermission(api = "/product/{id}", action = "VIEW")
    public ApiResponse<?> getById(@PathVariable String id) {
        return ApiResponse.success(productService.getById(id));
    }

    @Operation(summary = "Tạo sản phẩm mới", description = "Thêm sản phẩm mới vào danh mục")
    @PostMapping
    @CheckPermission(api = "/product", action = "CREATE")
    public ApiResponse<?> create(@RequestBody ProductCreateRequest request) {
        return ApiResponse.success(productService.create(request));
    }

    @Operation(summary = "Cập nhật sản phẩm", description = "Cập nhật thông tin sản phẩm đã có")
    @PutMapping("/{id}")
    @CheckPermission(api = "/product/{id}", action = "UPDATE")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody ProductUpdateRequest request) {
        return ApiResponse.success(productService.update(id, request));
    }

    @Operation(summary = "Xóa sản phẩm", description = "Xóa mềm sản phẩm khỏi hệ thống")
    @DeleteMapping("/{id}")
    @CheckPermission(api = "/product/{id}", action = "DELETE")
    public ApiResponse<?> delete(@PathVariable String id) {
        productService.delete(id);
        return ApiResponse.success("Xóa sản phẩm thành công");
    }

    @Operation(summary = "Cập nhật giá hàng loạt", description = "Cập nhật giá cho nhiều sản phẩm và đơn vị tính")
    @PostMapping("/bulk-update-prices")
    @CheckPermission(api = "/product/bulk-update-prices", action = "UPDATE")
    public ApiResponse<?> bulkUpdatePrices(@RequestBody java.util.List<com.frezo.product.dto.request.PriceUpdateRequest> requests) {
        productService.bulkUpdatePrices(requests);
        return ApiResponse.success("Cập nhật giá thành công");
    }

    @Operation(summary = "Nhập kho nhanh theo lô", description = "Nhập nhiều sản phẩm cùng lúc vào kho")
    @PostMapping("/import-batch")
    @CheckPermission(api = "/product/import-batch", action = "CREATE")
    public ApiResponse<?> importBatch(@RequestBody com.frezo.product.dto.request.BatchImportRequest request) {
        productService.importBatch(request);
        return ApiResponse.success("Nhập kho thành công");
    }

    @Operation(summary = "Tính giá bán tự động", description = "Tính giá dựa trên sản phẩm, đơn vị và nhóm khách")
    @GetMapping("/calculate-price")
    public ApiResponse<?> calculatePrice(@RequestParam String productCode, @RequestParam String unitName, @RequestParam String priceGroupCode) {
        return ApiResponse.success(productService.calculatePrice(productCode, unitName, priceGroupCode));
    }

    @Operation(summary = "Lấy dữ liệu biểu đồ lợi nhuận", description = "Lấy doanh thu và giá vốn N ngày gần nhất")
    @GetMapping("/dashboard/profit-chart")
    public ApiResponse<?> getProfitChart(@RequestParam(defaultValue = "7") int days) {
        return ApiResponse.success(productService.getProfitChart(days));
    }

    @Operation(summary = "Lấy biến động giá", description = "Lấy top sản phẩm biến động giá so với hôm qua")
    @GetMapping("/dashboard/price-fluctuation")
    public ApiResponse<?> getPriceFluctuation() {
        return ApiResponse.success(productService.getPriceFluctuation());
    }

    @Operation(summary = "So sánh giá thị trường", description = "So sánh giá bán của cửa hàng với giá chợ đầu mối")
    @GetMapping("/dashboard/market-comparison")
    public ApiResponse<?> getMarketComparison() {
        return ApiResponse.success(productService.getMarketComparison());
    }
}
