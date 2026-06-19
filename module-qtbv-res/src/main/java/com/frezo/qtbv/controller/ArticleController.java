package com.frezo.qtbv.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qtbv.config.SecurityHelper;
import com.frezo.qtbv.dto.request.ArticleCreateRequest;
import com.frezo.qtbv.dto.request.ArticleFilterRequest;
import com.frezo.qtbv.dto.request.ArticleReviewRequest;
import com.frezo.qtbv.dto.request.ArticleUpdateRequest;
import com.frezo.qtbv.service.ArticleService;
import com.frezo.qtht.config.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qtbv/articles")
@RequiredArgsConstructor
@Tag(name = "7. Bài viết", description = "API quản lý bài viết")
public class ArticleController {

    private final ArticleService articleService;
    private final SecurityHelper securityHelper;

    @Operation(summary = "Lọc danh sách bài viết", description = "Lọc bài viết theo nhiều tiêu chí")
    @PostMapping("/filter")
    @CheckPermission(api = "/qtbv/articles/filter", action = "VIEW")
    public ApiResponse<?> filter(@RequestBody(required = false) ArticleFilterRequest request) {
        return ApiResponse.success(articleService.filter(request));
    }

    @Operation(summary = "Xem chi tiết bài viết", description = "Xem thông tin chi tiết của bài viết")
    @GetMapping("/{id}")
    @CheckPermission(api = "/qtbv/articles", action = "VIEW")
    public ApiResponse<?> findById(@PathVariable String id) {
        return ApiResponse.success(articleService.findById(id));
    }

    @Operation(summary = "Tạo bài viết mới", description = "Tạo bài viết mới với trạng thái DRAFT")
    @PostMapping
    @CheckPermission(api = "/qtbv/articles", action = "CREATE")
    public ApiResponse<?> create(@Valid @RequestBody ArticleCreateRequest request) {
        return ApiResponse.success(articleService.create(request));
    }

    @Operation(summary = "Cập nhật bài viết", description = "Cập nhật bài viết (chỉ khi ở trạng thái DRAFT hoặc REJECTED)")
    @PutMapping("/{id}")
    @CheckPermission(api = "/qtbv/articles", action = "UPDATE")
    public ApiResponse<?> update(@PathVariable String id, @Valid @RequestBody ArticleUpdateRequest request) {
        return ApiResponse.success(articleService.update(id, request, securityHelper.getCurrentUserId()));
    }

    @Operation(summary = "Xóa bài viết", description = "Xóa mềm bài viết")
    @DeleteMapping("/{id}")
    @CheckPermission(api = "/qtbv/articles", action = "DELETE")
    public ApiResponse<?> delete(@PathVariable String id) {
        articleService.delete(id, securityHelper.getCurrentUserId());
        return ApiResponse.success(null);
    }

    @Operation(summary = "Gửi duyệt bài viết", description = "Gửi bài viết cho quản lý duyệt")
    @PutMapping("/{id}/submit")
    @CheckPermission(api = "/qtbv/articles/submit", action = "UPDATE")
    public ApiResponse<?> submitForApproval(@PathVariable String id) {
        articleService.submitForApproval(id, securityHelper.getCurrentUserId());
        return ApiResponse.success(null);
    }

    @Operation(summary = "Xuất bản bài viết", description = "Xuất bản bài viết đã được duyệt")
    @PutMapping("/{id}/publish")
    @CheckPermission(api = "/qtbv/articles/publish", action = "UPDATE")
    public ApiResponse<?> publish(@PathVariable String id) {
        return ApiResponse.success(articleService.publish(id, securityHelper.getCurrentUserId()));
    }

    @Operation(summary = "Duyệt/từ chối bài viết", description = "Quản lý duyệt hoặc từ chối bài viết")
    @PutMapping("/{id}/review")
    @CheckPermission(api = "/qtbv/articles/review", action = "UPDATE")
    public ApiResponse<?> review(@PathVariable String id, @Valid @RequestBody ArticleReviewRequest request) {
        return ApiResponse.success(articleService.review(id, request, securityHelper.getCurrentUserId()));
    }

    @Operation(summary = "Bài viết của tôi", description = "Lấy danh sách bài viết nháp của tác giả")
    @GetMapping("/my-drafts")
    @CheckPermission(api = "/qtbv/articles/my-drafts", action = "VIEW")
    public ApiResponse<?> getMyDrafts(@RequestParam(name = "page", defaultValue = "0") int page,
                                      @RequestParam(name = "size", defaultValue = "10") int size) {
        return ApiResponse.success(articleService.getMyDrafts(page, size, securityHelper.getCurrentUserId()));
    }

    @Operation(summary = "Bài viết chờ duyệt", description = "Lấy danh sách bài viết chờ duyệt của quản lý")
    @GetMapping("/pending-approval")
    @CheckPermission(api = "/qtbv/articles/pending-approval", action = "VIEW")
    public ApiResponse<?> getPendingApproval(@RequestParam(name = "page", defaultValue = "0") int page,
                                             @RequestParam(name = "size", defaultValue = "10") int size) {
        return ApiResponse.success(articleService.getPendingApproval(page, size, securityHelper.getCurrentUserId()));
    }

    @Operation(summary = "Bài viết đã xuất bản", description = "Lấy danh sách bài viết đã xuất bản theo tổ chức")
    @GetMapping("/published")
    @CheckPermission(api = "/qtbv/articles/published", action = "VIEW")
    public ApiResponse<?> getPublishedArticles(@RequestParam(name = "organizationId", required = false) String organizationId) {
        return ApiResponse.success(articleService.getPublishedArticles(organizationId));
    }
}
