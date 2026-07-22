package com.frezo.fbautomation.controller;

import com.frezo.fbautomation.dto.request.SocialPostRequest;
import com.frezo.fbautomation.dto.response.SocialPostResponse;
import com.frezo.fbautomation.service.SocialPostService;
import com.frezo.util.web.Response;
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
    @Operation(summary = "List bài viết")
    public Response<List<SocialPostResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel) {
        return Response.ok(service.list(status, channel));
    }

    @GetMapping("/{id}")
    public Response<SocialPostResponse> get(@PathVariable String id) {
        return Response.ok(service.get(id));
    }

    @PostMapping
    @Operation(summary = "Tạo bản nháp hoặc lên lịch")
    public Response<SocialPostResponse> create(@RequestBody @Valid SocialPostRequest req) {
        return Response.ok(service.create(req));
    }

    @PutMapping("/{id}")
    public Response<SocialPostResponse> update(
            @PathVariable String id,
            @RequestBody @Valid SocialPostRequest req) {
        return Response.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable String id) {
        service.delete(id);
        return Response.ok();
    }

    @PostMapping("/{id}/duplicate")
    public Response<SocialPostResponse> duplicate(@PathVariable String id) {
        return Response.ok(service.duplicate(id));
    }

    @PostMapping("/{id}/cancel")
    public Response<SocialPostResponse> cancel(@PathVariable String id) {
        return Response.ok(service.cancel(id));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish ngay lập tức (cần Page Access Token)")
    public Response<SocialPostResponse> publishNow(@PathVariable String id) {
        return Response.ok(service.publishNow(id));
    }
}
