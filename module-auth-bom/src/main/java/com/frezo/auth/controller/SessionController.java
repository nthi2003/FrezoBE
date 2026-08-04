package com.frezo.auth.controller;

import com.frezo.auth.entity.UserSession;
import com.frezo.auth.service.UserSessionService;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.response.ApiResponse;
import com.frezo.common.security.CheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth/session")
@RequiredArgsConstructor
@Tag(name = "Session Management", description = "Quản lý phiên đăng nhập người dùng")
public class SessionController {

    private final UserSessionService userSessionService;

    @Operation(summary = "Lấy danh sách phiên đăng nhập đang hoạt động", description = "Lấy tất cả các phiên đăng nhập đang hoạt động của người dùng hiện tại")
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<UserSession>>> getActiveSessions() {
        String username = SystemUtils.getCurrentUsername();
        return ResponseEntity.ok(ApiResponse.success(userSessionService.getActiveSessions(username)));
    }

    @Operation(summary = "Lấy danh sách phiên đăng nhập đang hoạt động (phân trang)", description = "Lấy các phiên đăng nhập đang hoạt động với phân trang")
    @GetMapping("/active/paged")
    public ResponseEntity<ApiResponse<Page<UserSession>>> getActiveSessionsPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        String username = SystemUtils.getCurrentUsername();
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("loginTime").descending());
        return ResponseEntity.ok(ApiResponse.success(userSessionService.getActiveSessions(username, pageRequest)));
    }

    @Operation(summary = "Thu hồi một phiên đăng nhập", description = "Vô hiệu hóa một phiên đăng nhập cụ thể")
    @PostMapping("/revoke/{id}")
    public ResponseEntity<ApiResponse<String>> revokeSession(@PathVariable String id) {
        String revokedBy = SystemUtils.getCurrentUsername();
        userSessionService.revokeSession(id, revokedBy);
        return ResponseEntity.ok(ApiResponse.success("Phiên đăng nhập đã bị thu hồi"));
    }

    @Operation(summary = "Thu hồi tất cả phiên đăng nhập khác", description = "Vô hiệu hóa tất cả các phiên đăng nhập khác ngoài phiên hiện tại")
    @PostMapping("/revoke-all")
    public ResponseEntity<ApiResponse<String>> revokeAllOtherSessions(@RequestParam String currentSessionId) {
        String username = SystemUtils.getCurrentUsername();
        userSessionService.revokeAllOtherSessions(username, currentSessionId, username);
        return ResponseEntity.ok(ApiResponse.success("Tất cả phiên đăng nhập khác đã bị thu hồi"));
    }

    @Operation(summary = "Đếm số phiên đăng nhập đang hoạt động", description = "Lấy số lượng phiên đăng nhập đang hoạt động của người dùng")
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countActiveSessions() {
        String username = SystemUtils.getCurrentUsername();
        return ResponseEntity.ok(ApiResponse.success(userSessionService.countActiveSessions(username)));
    }

    @Operation(summary = "Heartbeat phiên", description = "Cập nhật lastActiveTime cho session JWT hiện tại (FE ping mỗi 1–2 phút)")
    @PostMapping("/heartbeat")
    public ResponseEntity<ApiResponse<Map<String, Object>>> heartbeat(HttpServletRequest request) {
        String token = extractBearer(request);
        boolean ok = userSessionService.heartbeat(token);
        return ResponseEntity.ok(ApiResponse.success(Map.of("ok", ok)));
    }

    @Operation(summary = "Số user đang online", description = "Distinct username có lastActive trong N phút (mặc định 5)")
    @GetMapping("/online-count")
    @CheckPermission(api = "/auth/session/online-count", action = "VIEW")
    public ResponseEntity<ApiResponse<Map<String, Long>>> onlineCount(
            @RequestParam(defaultValue = "5") int minutes) {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "onlineUsers", userSessionService.countOnlineUsers(minutes),
                "activeSessions", userSessionService.countAllActiveSessions()
        )));
    }

    @Operation(summary = "Danh sách phiên toàn hệ thống (admin)", description = "Phân trang mọi session đang active")
    @GetMapping("/admin/active")
    @CheckPermission(api = "/auth/session/admin/active", action = "VIEW")
    public ResponseEntity<ApiResponse<Page<UserSession>>> adminActiveSessions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("lastActiveTime").descending());
        return ResponseEntity.ok(ApiResponse.success(userSessionService.getAllActiveSessions(pageRequest)));
    }

    private static String extractBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
