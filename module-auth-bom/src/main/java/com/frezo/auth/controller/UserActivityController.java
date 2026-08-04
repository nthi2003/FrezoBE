package com.frezo.auth.controller;

import com.frezo.auth.service.UserActivityService;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth/statistic")
@RequiredArgsConstructor
@Tag(name = "1. Xác thực (Auth)", description = "API thống kê hoạt động")
public class UserActivityController {

    private final UserActivityService userActivityService;

    @Operation(summary = "Thống kê đăng nhập theo ngày", description = "Lấy số lượng login thành công trong 30 ngày gần nhất")
    @GetMapping("/login-by-day")
    @CheckPermission(api = "/auth/statistic/login-by-day", action = "VIEW")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getLoginStatistics() {
        return ResponseEntity.ok(ApiResponse.success(userActivityService.loginByDayLast30()));
    }

    @Operation(summary = "Tóm tắt usage hôm nay", description = "Login hôm nay + online + phiên active — cho hub /qtht/usage")
    @GetMapping("/usage-summary")
    @CheckPermission(api = "/auth/statistic/usage-summary", action = "VIEW")
    public ResponseEntity<ApiResponse<Map<String, Object>>> usageSummary(
            @RequestParam(defaultValue = "5") int onlineMinutes) {
        return ResponseEntity.ok(ApiResponse.success(userActivityService.usageSummary(onlineMinutes)));
    }
}
