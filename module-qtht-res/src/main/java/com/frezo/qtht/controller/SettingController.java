package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qtht.config.CheckPermission;
import com.frezo.qtht.dto.request.SettingAddRequest;
import com.frezo.qtht.dto.request.SettingEditRequest;
import com.frezo.qtht.service.SettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qtht/setting")
@RequiredArgsConstructor
@Tag(name = "SettingController", description = "Quản lý cấu hình tính năng theo tổ chức")
public class SettingController {

    private final SettingService settingService;

    @Operation(summary = "Lấy danh sách cấu hình", description = "Lấy tất cả cấu hình hệ thống")
    @GetMapping
    @CheckPermission(api = "/qtht/setting", action = "VIEW")
    public ApiResponse<?> getAll() {
        return ApiResponse.success(settingService.findAll());
    }

    @Operation(summary = "Thêm mới cấu hình", description = "Tạo cấu hình tính năng mới cho một tổ chức")
    @PostMapping
    @CheckPermission(api = "/qtht/setting", action = "CREATE")
    public ApiResponse<?> create(@Valid @RequestBody SettingAddRequest request) {
        return ApiResponse.success(settingService.add(request));
    }

    @Operation(summary = "Cập nhật cấu hình", description = "Cập nhật các flag tính năng cho tổ chức")
    @PutMapping("/{id}")
    @CheckPermission(api = "/qtht/setting", action = "UPDATE")
    public ApiResponse<?> update(@PathVariable String id, @Valid @RequestBody SettingEditRequest request) {
        request.setId(id);
        return ApiResponse.success(settingService.edit(request));
    }

    @Operation(summary = "Lấy cấu hình theo orgId", description = "Lấy thông tin cấu hình tính năng của một tổ chức cụ thể")
    @GetMapping("/org/{orgId}")
    @CheckPermission(api = "/qtht/setting", action = "VIEW")
    public ApiResponse<?> getByOrgId(@PathVariable String orgId) {
        return ApiResponse.success(settingService.getByOrgId(orgId));
    }
}
