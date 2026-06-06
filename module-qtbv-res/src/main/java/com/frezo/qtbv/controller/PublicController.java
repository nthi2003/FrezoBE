package com.frezo.qtbv.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.product.dto.request.ProductFilterRequest;
import com.frezo.qtbv.service.PublicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Tag(name = "0. Public API", description = "Tổng hợp các API công khai cho Landing Page (không cần token)")
public class PublicController {

    private final PublicService publicService;

    @Operation(summary = "Lấy cấu hình Landing Page", description = "Lấy toàn bộ thông tin brand, slogan, liên hệ")
    @GetMapping("/landing/config")
    public ApiResponse<?> getLandingConfig() {
        return ApiResponse.success(publicService.getLandingConfig());
    }

    @Operation(summary = "Lấy danh sách sản phẩm", description = "Lọc danh sách sản phẩm cho khách hàng")
    @PostMapping("/product/filter")
    public ApiResponse<?> filterProducts(@RequestBody(required = false) ProductFilterRequest filterRequest) {
        return ApiResponse.success(publicService.getProducts(filterRequest));
    }

    @Operation(summary = "Lấy chi tiết sản phẩm", description = "Xem chi tiết sản phẩm cho khách hàng")
    @GetMapping("/product/{id}")
    public ApiResponse<?> getProductDetail(@PathVariable String id) {
        return ApiResponse.success(publicService.getProductDetail(id));
    }

    @Operation(summary = "Lấy danh sách bài viết", description = "Lấy danh sách bài viết công khai")
    @GetMapping("/articles")
    public ApiResponse<?> getArticles(@RequestParam(name = "page", defaultValue = "0") int page,
                                     @RequestParam(name = "size", defaultValue = "10") int size) {
        return ApiResponse.success(publicService.getArticles(page, size));
    }

    @Operation(summary = "Lấy chi tiết bài viết", description = "Xem chi tiết bài viết công khai")
    @GetMapping("/articles/{id}")
    public ApiResponse<?> getArticleDetail(@PathVariable String id) {
        return ApiResponse.success(publicService.getArticleDetail(id));
    }
}
