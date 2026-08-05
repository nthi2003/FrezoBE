package com.frezo.qtbv.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.qtbv.entity.Banner;
import com.frezo.qtbv.service.BannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qtbv/banners")
@RequiredArgsConstructor
@Tag(name = "7. Banner Landing", description = "API quản lý banner hiển thị trên landing page")
public class BannerController {

    private final BannerService bannerService;

    @Operation(summary = "Danh sách banner")
    @GetMapping
    @CheckPermission(api = "/qtbv/banners", action = "VIEW")
    public ApiResponse<?> list() {
        return ApiResponse.success(bannerService.findAll());
    }

    @Operation(summary = "Chi tiết banner")
    @GetMapping("/{id}")
    @CheckPermission(api = "/qtbv/banners/{id}", action = "VIEW")
    public ApiResponse<?> findById(@PathVariable String id) {
        return ApiResponse.success(bannerService.findById(id));
    }

    @Operation(summary = "Tạo banner")
    @PostMapping
    @CheckPermission(api = "/qtbv/banners", action = "CREATE")
    public ApiResponse<?> create(@RequestBody Banner request) {
        return ApiResponse.success(bannerService.create(request));
    }

    @Operation(summary = "Cập nhật banner")
    @PutMapping("/{id}")
    @CheckPermission(api = "/qtbv/banners/{id}", action = "UPDATE")
    public ApiResponse<?> update(@PathVariable String id, @RequestBody Banner request) {
        return ApiResponse.success(bannerService.update(id, request));
    }

    @Operation(summary = "Xóa banner (soft-delete)")
    @DeleteMapping("/{id}")
    @CheckPermission(api = "/qtbv/banners/{id}", action = "DELETE")
    public ApiResponse<?> delete(@PathVariable String id) {
        bannerService.delete(id);
        return ApiResponse.success(null);
    }
}
