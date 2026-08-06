package com.frezo.qtbv.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.qtbv.dto.request.ArticlePinRequest;
import com.frezo.qtbv.dto.request.NewsCategoryRequest;
import com.frezo.qtbv.dto.request.NewsMottoRequest;
import com.frezo.qtbv.service.NewsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qtbv/news")
@RequiredArgsConstructor
@Tag(name = "7. Tin tức nội bộ", description = "Danh mục, châm ngôn, ghim tin và dữ liệu trang /bai-viet")
public class NewsController {

    private final NewsService newsService;

    @Operation(summary = "Dữ liệu trang Tin tức (banner, ghim, danh mục, bài viết)")
    @GetMapping("/page-data")
    @CheckPermission(api = "/qtbv/articles/home-feed", action = "VIEW")
    public ApiResponse<?> pageData(@RequestParam(name = "organizationId", required = false) String organizationId) {
        return ApiResponse.success(newsService.getNewsPageData(organizationId));
    }

    @Operation(summary = "Danh sách danh mục tin tức")
    @GetMapping("/categories")
    @CheckPermission(api = "/qtbv/articles", action = "VIEW")
    public ApiResponse<?> listCategories(@RequestParam(name = "organizationId", required = false) String organizationId) {
        return ApiResponse.success(newsService.listCategories(organizationId));
    }

    @Operation(summary = "Tạo danh mục tin tức")
    @PostMapping("/categories")
    @CheckPermission(api = "/qtbv/articles", action = "CREATE")
    public ApiResponse<?> createCategory(@RequestBody NewsCategoryRequest request) {
        return ApiResponse.success(newsService.createCategory(request));
    }

    @Operation(summary = "Cập nhật danh mục tin tức")
    @PutMapping("/categories/{id}")
    @CheckPermission(api = "/qtbv/articles/{id}", action = "UPDATE")
    public ApiResponse<?> updateCategory(@PathVariable String id, @RequestBody NewsCategoryRequest request) {
        return ApiResponse.success(newsService.updateCategory(id, request));
    }

    @Operation(summary = "Xóa danh mục tin tức")
    @DeleteMapping("/categories/{id}")
    @CheckPermission(api = "/qtbv/articles/{id}", action = "DELETE")
    public ApiResponse<?> deleteCategory(@PathVariable String id) {
        newsService.deleteCategory(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Danh sách châm ngôn")
    @GetMapping("/mottos")
    @CheckPermission(api = "/qtbv/articles", action = "VIEW")
    public ApiResponse<?> listMottos() {
        return ApiResponse.success(newsService.listMottos());
    }

    @Operation(summary = "Tạo châm ngôn")
    @PostMapping("/mottos")
    @CheckPermission(api = "/qtbv/articles", action = "CREATE")
    public ApiResponse<?> createMotto(@RequestBody NewsMottoRequest request) {
        return ApiResponse.success(newsService.createMotto(request));
    }

    @Operation(summary = "Cập nhật châm ngôn")
    @PutMapping("/mottos/{id}")
    @CheckPermission(api = "/qtbv/articles/{id}", action = "UPDATE")
    public ApiResponse<?> updateMotto(@PathVariable String id, @RequestBody NewsMottoRequest request) {
        return ApiResponse.success(newsService.updateMotto(id, request));
    }

    @Operation(summary = "Xóa châm ngôn")
    @DeleteMapping("/mottos/{id}")
    @CheckPermission(api = "/qtbv/articles/{id}", action = "DELETE")
    public ApiResponse<?> deleteMotto(@PathVariable String id) {
        newsService.deleteMotto(id);
        return ApiResponse.success(null);
    }

    @Operation(summary = "Danh sách tin ghim theo đơn vị")
    @GetMapping("/pins")
    @CheckPermission(api = "/qtbv/articles", action = "VIEW")
    public ApiResponse<?> listPins(@RequestParam(name = "organizationId") String organizationId) {
        return ApiResponse.success(newsService.listPins(organizationId));
    }

    @Operation(summary = "Ghim tin (tối đa 5/đơn vị)")
    @PostMapping("/pins")
    @CheckPermission(api = "/qtbv/articles", action = "UPDATE")
    public ApiResponse<?> pinArticle(@RequestBody ArticlePinRequest request) {
        return ApiResponse.success(newsService.pinArticle(request));
    }

    @Operation(summary = "Bỏ ghim tin")
    @DeleteMapping("/pins")
    @CheckPermission(api = "/qtbv/articles/{id}", action = "UPDATE")
    public ApiResponse<?> unpinArticle(
            @RequestParam(name = "organizationId") String organizationId,
            @RequestParam(name = "articleId") String articleId) {
        newsService.unpinArticle(organizationId, articleId);
        return ApiResponse.success(null);
    }
}
