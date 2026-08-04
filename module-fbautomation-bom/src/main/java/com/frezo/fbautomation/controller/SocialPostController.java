package com.frezo.fbautomation.controller;

import com.frezo.fbautomation.dto.request.SocialPostRequest;
import com.frezo.fbautomation.dto.response.SocialPostResponse;
import com.frezo.fbautomation.service.SocialPostService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * SocialPostController — CRUD + scheduler control cho bài đăng đa kênh.
 * <p>
 * Endpoints:
 * - GET    /mkt/posts                — list (filter status/channel)
 * - GET    /mkt/posts/{id}           — chi tiết
 * - POST   /mkt/posts                — tạo bản nháp hoặc lên lịch
 * - PUT    /mkt/posts/{id}           — cập nhật
 * - DELETE /mkt/posts/{id}           — xóa mềm
 * - POST   /mkt/posts/{id}/duplicate — nhân bản (A/B test)
 * - POST   /mkt/posts/{id}/cancel    — hủy lịch đăng
 * - POST   /mkt/posts/{id}/publish   — đăng ngay
 */
@RestController
@RequestMapping("/mkt/posts")
@RequiredArgsConstructor
@Tag(name = "MKT · Content Scheduler", description = "Lên lịch đăng bài đa kênh (FB/Zalo/IG)")
public class SocialPostController {

    private final SocialPostService service;

    @GetMapping
    @CheckPermission(api = "/mkt/posts", action = "VIEW")
    @Operation(summary = "List bài viết")
    public ApiResponse<List<SocialPostResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel) {
        return ApiResponse.ok(service.list(status, channel));
    }

    @GetMapping("/{id}")
    @CheckPermission(api = "/mkt/posts/{id}", action = "VIEW")
    public ApiResponse<SocialPostResponse> get(@PathVariable String id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @CheckPermission(api = "/mkt/posts", action = "CREATE")
    @Operation(summary = "Tạo bản nháp hoặc lên lịch")
    public ApiResponse<SocialPostResponse> create(@RequestBody @Valid SocialPostRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    @PutMapping("/{id}")
    @CheckPermission(api = "/mkt/posts/{id}", action = "UPDATE")
    public ApiResponse<SocialPostResponse> update(
            @PathVariable String id,
            @RequestBody @Valid SocialPostRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/mkt/posts/{id}", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/duplicate")
    @CheckPermission(api = "/mkt/posts/{id}/duplicate", action = "CREATE")
    public ApiResponse<SocialPostResponse> duplicate(@PathVariable String id) {
        return ApiResponse.ok(service.duplicate(id));
    }

    @PostMapping("/{id}/cancel")
    @CheckPermission(api = "/mkt/posts/{id}/cancel", action = "UPDATE")
    public ApiResponse<SocialPostResponse> cancel(@PathVariable String id) {
        return ApiResponse.ok(service.cancel(id));
    }

    @PostMapping("/{id}/publish")
    @CheckPermission(api = "/mkt/posts/{id}/publish", action = "UPDATE")
    @Operation(summary = "Publish ngay lập tức (cần Page Access Token)")
    public ApiResponse<SocialPostResponse> publishNow(@PathVariable String id) {
        return ApiResponse.ok(service.publishNow(id));
    }
}
