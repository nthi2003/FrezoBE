package com.frezo.fbautomation.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.fbautomation.dto.request.PageReviewRequest;
import com.frezo.fbautomation.dto.response.PageReviewResponse;
import com.frezo.fbautomation.service.PageReviewService;
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
import java.util.Map;

@RestController
@RequestMapping("/mkt/reviews")
@RequiredArgsConstructor
@Tag(name = "MKT · Reviews", description = "Theo dõi đánh giá page (MVP nhập tay)")
public class PageReviewController {

    private final PageReviewService service;

    @GetMapping("/dashboard")
    @CheckPermission(api = "/mkt/reviews/dashboard", action = "VIEW")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(service.dashboard());
    }

    @GetMapping
    @CheckPermission(api = "/mkt/reviews", action = "VIEW")
    public ApiResponse<List<PageReviewResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String platform) {
        return ApiResponse.ok(service.list(status, platform));
    }

    @GetMapping("/{id}")
    @CheckPermission(api = "/mkt/reviews/{id}", action = "VIEW")
    public ApiResponse<PageReviewResponse> get(@PathVariable String id) {
        return ApiResponse.ok(service.get(id));
    }

    @PostMapping
    @CheckPermission(api = "/mkt/reviews", action = "CREATE")
    public ApiResponse<PageReviewResponse> create(@RequestBody @Valid PageReviewRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    @PutMapping("/{id}")
    @CheckPermission(api = "/mkt/reviews/{id}", action = "UPDATE")
    public ApiResponse<PageReviewResponse> update(
            @PathVariable String id,
            @RequestBody @Valid PageReviewRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    @CheckPermission(api = "/mkt/reviews/{id}", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/reply")
    @CheckPermission(api = "/mkt/reviews/{id}/reply", action = "UPDATE")
    @Operation(summary = "Trả lời đánh giá")
    public ApiResponse<PageReviewResponse> reply(
            @PathVariable String id,
            @RequestParam String replyText) {
        return ApiResponse.ok(service.reply(id, replyText));
    }
}
