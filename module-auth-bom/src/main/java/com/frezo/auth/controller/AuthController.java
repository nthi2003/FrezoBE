package com.frezo.auth.controller;

import com.frezo.auth.dto.request.LoginRequest;
import com.frezo.auth.dto.response.LoginResponse;
import com.frezo.auth.entity.LoginHistory;
import com.frezo.auth.service.AuthService;
import com.frezo.common.helper.SystemUtils;
import com.frezo.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication Management", description = "Quản lý xác thực và thông tin người dùng")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Đăng nhập", description = "Xác thực người dùng bằng username và password")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.login(request)));
    }

    @Operation(summary = "Xác thực OTP", description = "Xác thực mã OTP gửi về cho người dùng (2FA)")
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<LoginResponse>> verifyOtp(@RequestParam String username, @RequestParam String code) {
        return ResponseEntity.ok(ApiResponse.success(authService.verifyOtp(username, code)));
    }

    @Operation(summary = "Quên mật khẩu", description = "Yêu cầu khôi phục mật khẩu qua email")
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@RequestParam String email) {
        authService.forgotPassword(email);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Đặt lại mật khẩu", description = "Đặt lại mật khẩu mới bằng mã khôi phục")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestParam String key, @RequestParam String newPassword) {
        authService.resetPassword(key, newPassword);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "Lấy lịch sử đăng nhập", description = "Xem lịch sử các lần đăng nhập của người dùng")
    @GetMapping("/login-history")
    public ResponseEntity<ApiResponse<List<LoginHistory>>> getLoginHistory() {
        String username = SystemUtils.getCurrentUsername();
        return ResponseEntity.ok(ApiResponse.success(authService.getLoginHistory(username)));
    }

    @Operation(summary = "Làm mới Token", description = "Lấy Access Token mới bằng Refresh Token")
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@RequestParam String refreshToken) {
        return ResponseEntity.ok(ApiResponse.success(authService.refreshToken(refreshToken)));
    }

    @Operation(summary = "Lấy thông tin cá nhân", description = "Xem thông tin tài khoản đang đăng nhập")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Object>> getProfile() {
        return ResponseEntity.ok(ApiResponse.success(authService.getProfile()));
    }

    @Operation(summary = "Upload avatar", description = "Upload ảnh đại diện cho người dùng, lưu tại MinIO path frezo-user/avatar/{userName}")
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        String username = SystemUtils.getCurrentUsername();
        return ResponseEntity.ok(ApiResponse.success(authService.uploadAvatar(file, username)));
    }

    @Operation(summary = "Đăng xuất", description = "Đăng xuất tài khoản hiện tại")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String username = SystemUtils.getCurrentUsername();
        authService.logout(token, username);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
