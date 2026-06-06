package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.service.NotificationService;
import com.frezo.common.entity.Notification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "X. Thông báo (Notifications)", description = "API quản lý thông báo trong app")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Lấy danh sách thông báo", description = "Lấy toàn bộ thông báo của người dùng hiện tại")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<Notification>>> getMyNotifications(@RequestParam String username) {
        return ResponseEntity.ok(ApiResponse.success(notificationService.getMyNotifications(username)));
    }

    @Operation(summary = "Đánh dấu đã đọc", description = "Đánh dấu một thông báo là đã đọc")
    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đã đọc"));
    }
}
