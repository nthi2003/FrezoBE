package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.service.NotificationService;
import com.frezo.common.entity.Notification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/qtht/notification")
@RequiredArgsConstructor
@Tag(name = "X. Thông báo (Notifications)", description = "API quản lý thông báo trong app")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Lấy danh sách thông báo", description = "Lấy toàn bộ thông báo của người dùng hiện tại")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Notification>>> getMyNotifications(Authentication authentication) {
        String username = authentication.getName();
        return ResponseEntity.ok(ApiResponse.success(notificationService.getMyNotifications(username)));
    }

    @Operation(summary = "Đếm số thông báo chưa đọc", description = "Số badge hiển thị trên chuông")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(Authentication authentication) {
        long count = notificationService.getUnreadCount(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    @Operation(summary = "Đánh dấu đã đọc", description = "Đánh dấu một thông báo là đã đọc")
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đã đọc"));
    }

    @Operation(summary = "Đánh dấu tất cả đã đọc")
    @PatchMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead(Authentication authentication) {
        int n = notificationService.markAllAsRead(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(Map.of("updated", n)));
    }

    @Operation(summary = "Đánh dấu tất cả đã đọc (POST alias)")
    @PostMapping("/mark-all-read")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllReadPost(Authentication authentication) {
        int n = notificationService.markAllAsRead(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(Map.of("updated", n)));
    }
}
