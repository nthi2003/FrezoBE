package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.qtht.dto.request.GuideSaveRequest;
import com.frezo.qtht.dto.response.GuideResponse;
import com.frezo.qtht.dto.response.GuideSummaryResponse;
import com.frezo.qtht.service.GuideService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Guide CMS — FR-DOC-03.
 * <ul>
 *   <li>Published list/get: mọi user đã đăng nhập</li>
 *   <li>CRUD / publish: Admin (Person.isAdmin bypass) hoặc permission {@code /qtht/guides}</li>
 * </ul>
 */
@RestController
@RequestMapping("/qtht/guides")
@RequiredArgsConstructor
@Tag(name = "Guide CMS", description = "Quản lý hướng dẫn Docs Hub (FR-DOC-03)")
public class GuideController {

    private final GuideService guideService;

    // ── Public (authenticated) ──────────────────────────────────────────

    @GetMapping("/published")
    @Operation(summary = "Danh sách hướng dẫn đã xuất bản")
    public ApiResponse<List<GuideSummaryResponse>> listPublished() {
        return ApiResponse.ok(guideService.listPublished());
    }

    @GetMapping("/published/{slug}")
    @Operation(summary = "Chi tiết hướng dẫn đã xuất bản theo slug")
    public ApiResponse<GuideResponse> getPublishedBySlug(@PathVariable String slug) {
        return ApiResponse.ok(guideService.getPublishedBySlug(slug));
    }

    // ── Admin / BA ──────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "Admin: danh sách tất cả hướng dẫn")
    @CheckPermission(api = "/qtht/guides", action = "VIEW")
    public ApiResponse<List<GuideSummaryResponse>> listAll() {
        return ApiResponse.ok(guideService.listAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Admin: chi tiết hướng dẫn theo id")
    @CheckPermission(api = "/qtht/guides", action = "VIEW")
    public ApiResponse<GuideResponse> getById(@PathVariable String id) {
        return ApiResponse.ok(guideService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Admin: tạo hướng dẫn")
    @CheckPermission(api = "/qtht/guides", action = "CREATE")
    public ApiResponse<GuideResponse> create(@Valid @RequestBody GuideSaveRequest request) {
        return ApiResponse.created(guideService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Admin: cập nhật hướng dẫn")
    @CheckPermission(api = "/qtht/guides", action = "UPDATE")
    public ApiResponse<GuideResponse> update(
            @PathVariable String id,
            @Valid @RequestBody GuideSaveRequest request) {
        return ApiResponse.ok(guideService.update(id, request));
    }

    @PutMapping("/{id}/publish")
    @Operation(summary = "Admin: xuất bản hướng dẫn")
    @CheckPermission(api = "/qtht/guides", action = "UPDATE")
    public ApiResponse<GuideResponse> publish(@PathVariable String id) {
        return ApiResponse.ok(guideService.publish(id));
    }

    @PutMapping("/{id}/unpublish")
    @Operation(summary = "Admin: gỡ xuất bản hướng dẫn")
    @CheckPermission(api = "/qtht/guides", action = "UPDATE")
    public ApiResponse<GuideResponse> unpublish(@PathVariable String id) {
        return ApiResponse.ok(guideService.unpublish(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Admin: xóa mềm hướng dẫn")
    @CheckPermission(api = "/qtht/guides", action = "DELETE")
    public ApiResponse<Void> delete(@PathVariable String id) {
        guideService.delete(id);
        return ApiResponse.ok();
    }
}
