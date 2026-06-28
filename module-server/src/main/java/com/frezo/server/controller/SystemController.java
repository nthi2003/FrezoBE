package com.frezo.server.controller;

import com.frezo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController("serverSystemController")
@RequestMapping("/api/system")
@Tag(name = "00. Hệ thống", description = "API kiểm tra trạng thái & thông tin ứng dụng")
public class SystemController {

    @Value("${spring.application.name:FrezoBE}")
    private String appName;

    @Operation(summary = "Kiểm tra health", description = "Trả về trạng thái hoạt động của hệ thống")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(result);
    }

    @Operation(summary = "Thông tin ứng dụng", description = "Trả về thông tin phiên bản, thời gian chạy, ...")
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> info() {
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("applicationName", appName);
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("uptime", formatUptime(runtime.getUptime()));
        info.put("startTime", new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                .format(new java.util.Date(runtime.getStartTime())));
        info.put("currentTime", LocalDateTime.now().toString());
        info.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        info.put("totalMemory", Runtime.getRuntime().totalMemory() / (1024 * 1024) + " MB");
        info.put("freeMemory", Runtime.getRuntime().freeMemory() / (1024 * 1024) + " MB");
        info.put("maxMemory", Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MB");

        return ApiResponse.success(info);
    }

    private String formatUptime(long millis) {
        long days = millis / (24 * 60 * 60 * 1000);
        millis %= (24 * 60 * 60 * 1000);
        long hours = millis / (60 * 60 * 1000);
        millis %= (60 * 60 * 1000);
        long minutes = millis / (60 * 1000);
        millis %= (60 * 1000);
        long seconds = millis / 1000;
        return String.format("%d ngày %02d:%02d:%02d", days, hours, minutes, seconds);
    }
}
