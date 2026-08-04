package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.qtht.service.UsageAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/qtht/usage")
@RequiredArgsConstructor
@Tag(name = "Usage Analytics", description = "Thống kê sử dụng ERP / pageview")
public class UsageAnalyticsController {

    private final UsageAnalyticsService usageAnalyticsService;

    public record PageViewRequest(String route, String moduleCode) {}

    @Operation(summary = "Ghi pageview", description = "FE gọi khi đổi route (debounce). Chỉ lưu route/module/user.")
    @PostMapping("/pageview")
    public ResponseEntity<ApiResponse<Void>> track(@RequestBody(required = false) PageViewRequest req) {
        if (req != null) {
            usageAnalyticsService.trackPageView(req.route(), req.moduleCode());
        }
        return ResponseEntity.ok(ApiResponse.ok());
    }

    @Operation(summary = "Top module / route hôm nay")
    @GetMapping("/pageviews/top")
    @CheckPermission(api = "/qtht/usage/pageviews/top", action = "VIEW")
    public ResponseEntity<ApiResponse<Map<String, Object>>> top(
            @RequestParam(defaultValue = "1") int days) {
        return ResponseEntity.ok(ApiResponse.success(usageAnalyticsService.topPageViews(days)));
    }
}
