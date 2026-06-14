package com.frezo.auth.controller;

import com.frezo.auth.repository.LoginHistoryRepository;
import com.frezo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth/statistic")
@RequiredArgsConstructor
@Tag(name = "1. Xác thực (Auth)", description = "API thống kê hoạt động")
public class UserActivityController {

    private final LoginHistoryRepository loginHistoryRepository;

    @Operation(summary = "Thống kê đăng nhập theo ngày", description = "Lấy số lượng login thành công trong 30 ngày gần nhất")
    @GetMapping("/login-by-day")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getLoginStatistics() {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        
        // This is a simplified aggregation. In a real app, use a native query for performance.
        var history = loginHistoryRepository.findAll().stream()
                .filter(h -> h.getLoginTime().toLocalDate().isAfter(thirtyDaysAgo))
                .filter(h -> "SUCCESS".equals(h.getStatus()))
                .collect(Collectors.groupingBy(
                        h -> h.getLoginTime().toLocalDate().toString(),
                        Collectors.counting()
                ));

        // Sort by date string
        Map<String, Long> sortedHistory = new LinkedHashMap<>();
        history.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(x -> sortedHistory.put(x.getKey(), x.getValue()));

        return ResponseEntity.ok(ApiResponse.success(sortedHistory));
    }
}
