package com.frezo.auth.service;

import com.frezo.auth.dto.request.LoginRequest;
import com.frezo.auth.dto.response.LoginResponse;
import com.frezo.auth.entity.LoginHistory;

import java.io.File;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    LoginResponse verifyOtp(String username, String code);
    void forgotPassword(String email);
    void resetPassword(String key, String newPassword);
    List<LoginHistory> getLoginHistory(String username);
    LoginResponse refreshToken(String refreshToken);
    String upload(File file, String username);
    String uploadAvatar(MultipartFile file, String username);
    void logout(String token, String username);
    Object getProfile();
}
