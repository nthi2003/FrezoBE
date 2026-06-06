package com.frezo.qtbv.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.qtbv.entity.LandingConfig;
import com.frezo.qtbv.service.LandingConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.frezo.qtht.config.CheckPermission;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qtbv/landing-config")
@RequiredArgsConstructor
@Tag(name = "7. Cấu hình Landing Page", description = "API cấu hình Landing Page cho TheNewFarm")
public class LandingConfigController {
    private final LandingConfigService landingConfigService;

    @Operation(summary = "Lấy cấu hình Landing Page", description = "Lấy cấu hình hiện tại đang active")
    @GetMapping
    public ApiResponse<?> getConfig() {
        return ApiResponse.success(landingConfigService.getConfig());
    }

    @Operation(summary = "Cập nhật cấu hình Landing Page", description = "Cập nhật thông tin brand, logo, liên hệ")
    @PutMapping
    @CheckPermission(api = "/qtbv/landing-config", action = "UPDATE")
    public ApiResponse<?> updateConfig(@RequestBody LandingConfig config) {
        return ApiResponse.success(landingConfigService.updateConfig(config));
    }
}
