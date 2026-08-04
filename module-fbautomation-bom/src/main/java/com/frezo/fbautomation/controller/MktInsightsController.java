package com.frezo.fbautomation.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.fbautomation.service.MktInsightsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/mkt/insights")
@RequiredArgsConstructor
@Tag(name = "MKT · Insights", description = "Dashboard marketing tổng hợp từ dữ liệu Frezo")
public class MktInsightsController {

    private final MktInsightsService service;

    @GetMapping("/dashboard")
    @CheckPermission(api = "/mkt/insights/dashboard", action = "VIEW")
    @Operation(summary = "Insights dashboard (aggregate leads/posts/ads/affiliate)")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.ok(service.dashboard());
    }
}
