package com.frezo.qtht.controller;

import com.frezo.common.response.ApiResponse;
import com.frezo.common.response.PageResponse;
import com.frezo.common.security.CheckPermission;
import com.frezo.common.service.NotificationService;
import com.frezo.common.entity.Notification;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/qtht/notification")
@RequiredArgsConstructor
@Tag(name = "X. Thông báo (Notifications)", description = "API quản lý thông báo trong app")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "Lấy danh sách thông báo ",
            description = "page; size bỏ trống = lấy hết trong 1 trang (tương thích bell/lobby). "
                    + "Filter: tab=all|unread|urgent, type, search.")
    @GetMapping("/my")
    @CheckPermission(api = "/qtht/notification/my", action = "VIEW")
    public ResponseEntity<ApiResponse<PageResponse<Notification>>> getMyNotifications(
            Authentication authentication,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String search) {
        String username = authentication.getName();
        PageResponse<Notification> result = notificationService.getMyNotifications(
                username, page, size, tab, type, search);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @Operation(summary = "Đếm số thông báo chưa đọc", description = "Số badge hiển thị trên chuông + stats trang")
    @GetMapping("/unread-count")
    @CheckPermission(api = "/qtht/notification/unread-count", action = "VIEW")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(Authentication authentication) {
        String username = authentication.getName();
        long unread = notificationService.getUnreadCount(username);
        long total = notificationService.getTotalCount(username);
        long urgent = notificationService.getUrgentUnreadCount(username);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "count", unread,
                "total", total,
                "urgent", urgent)));
    }

    @Operation(summary = "Đánh dấu đã đọc", description = "Đánh dấu một thông báo là đã đọc")
    @PatchMapping("/{id}/read")
    @CheckPermission(api = "/qtht/notification/{id}/read", action = "UPDATE")
    public ResponseEntity<ApiResponse<String>> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đã đọc"));
    }

    @Operation(summary = "Đánh dấu tất cả đã đọc")
    @PatchMapping("/mark-all-read")
    @CheckPermission(api = "/qtht/notification/mark-all-read", action = "UPDATE")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllRead(Authentication authentication) {
        int n = notificationService.markAllAsRead(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(Map.of("updated", n)));
    }

    @Operation(summary = "Đánh dấu tất cả đã đọc (POST alias)")
    @PostMapping("/mark-all-read")
    @CheckPermission(api = "/qtht/notification/mark-all-read", action = "CREATE")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllReadPost(Authentication authentication) {
        int n = notificationService.markAllAsRead(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(Map.of("updated", n)));
    }
}
