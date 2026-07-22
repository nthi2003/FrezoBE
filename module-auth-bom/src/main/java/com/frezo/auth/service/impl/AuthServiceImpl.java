package com.frezo.auth.service.impl;

import com.frezo.auth.dto.request.LoginRequest;
import com.frezo.auth.dto.response.LoginResponse;
import com.frezo.auth.entity.LoginHistory;
import com.frezo.auth.service.AuthService;
import com.frezo.auth.service.impl.auth.AuthLoginProcessor;
import com.frezo.auth.service.impl.auth.AuthPasswordResetService;
import com.frezo.auth.service.impl.auth.AuthProfileService;
import com.frezo.auth.service.impl.auth.AuthSessionService;
import com.frezo.auth.service.impl.auth.AuthTwoFactorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

/**
 * Façade cho tầng Auth.
 * <p>
 * Sau refactor Batch B (2026-07): giảm từ 14 deps → 5 deps bằng cách delegate cho các
 * component chuyên biệt trong package {@code service.impl.auth}:
 * <ul>
 *   <li>{@link AuthLoginProcessor} — login flow + 2FA trigger</li>
 *   <li>{@link AuthTwoFactorService} — sinh + verify OTP</li>
 *   <li>{@link AuthPasswordResetService} — forgot/reset password</li>
 *   <li>{@link AuthSessionService} — logout + login history</li>
 *   <li>{@link AuthProfileService} — profile + avatar</li>
 *   <li>{@code AuthTokenBuilder} — sinh JWT (dùng chung bởi LoginProcessor / TwoFactorService, không inject trực tiếp ở façade)</li>
 * </ul>
 * Interface {@link AuthService} public không đổi để controller cũ không break.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AuthLoginProcessor loginProcessor;
    private final AuthTwoFactorService twoFactorService;
    private final AuthPasswordResetService passwordResetService;
    private final AuthSessionService sessionService;
    private final AuthProfileService profileService;

    @Override
    public LoginResponse login(LoginRequest request) {
        return loginProcessor.login(request);
    }

    @Override
    public LoginResponse verifyOtp(String username, String code) {
        return twoFactorService.verifyOtp(username, code);
    }

    @Override
    public void forgotPassword(String email) {
        passwordResetService.forgotPassword(email);
    }

    @Override
    public void resetPassword(String key, String newPassword) {
        passwordResetService.resetPassword(key, newPassword);
    }

    @Override
    public List<LoginHistory> getLoginHistory(String username) {
        return sessionService.getRecentLoginHistory(username);
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        return loginProcessor.refreshToken(refreshToken);
    }

    @Override
    public String upload(File file, String username) {
        return profileService.upload(file, username);
    }

    @Override
    public String uploadAvatar(MultipartFile file, String username) {
        return profileService.uploadAvatar(file, username);
    }

    @Override
    public void logout(String token, String username) {
        sessionService.logout(token, username);
    }

    @Override
    public Object getProfile() {
        return profileService.getCurrentProfile();
    }
}
