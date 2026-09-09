package com.frezo.auth.service.impl.auth;

import com.frezo.auth.dto.response.LoginResponse;
import com.frezo.auth.entity.User;
import com.frezo.auth.repository.UserRepository;

import com.frezo.common.exception.QTHTException;
import com.frezo.common.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;


@Component
@RequiredArgsConstructor
public class AuthTwoFactorService {


    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuthTokenBuilder tokenBuilder;
    private final AuthSessionService sessionService;


    public LoginResponse startTwoFactor(User user) {
        int otpValue = 100_000 + SECURE_RANDOM.nextInt(900_000);
        String otp = String.valueOf(otpValue);
        user.setOtpCode(otp);
        user.setOtpExpiration(LocalDateTime.now().plusMinutes(5));
        userRepository.save(user);

        notificationService.notifyUserWithEmailFallback(user.getUserName(),
                "Mã xác thực 2FA",
                "Mã OTP của bạn là: " + otp + ". Hiệu lực trong 5 phút.",
                true);

        return LoginResponse.builder()
                .requiresTwoFactor(true)
                .message("Yêu cầu mã xác thực 2FA")
                .build();
    }

    /**
     * Verify OTP người dùng nhập lại. Nếu đúng, xoá OTP và trả cặp token mới.
     */
    public LoginResponse verifyOtp(String username, String code) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new QTHTException("user.not.found"));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(code)
                || user.getOtpExpiration() == null
                || user.getOtpExpiration().isBefore(LocalDateTime.now())) {
            throw new QTHTException("otp.exp");
        }

        user.setOtpCode(null);
        user.setOtpExpiration(null);
        userRepository.save(user);

        String ip = IpResolver.currentClientIp();
        String userAgent = IpResolver.currentUserAgent();
        String sessionId = sessionService.beginSession(username, ip, userAgent);
        LoginResponse response = tokenBuilder.buildTokensByUsername(username, "Xác thực thành công", sessionId);
        sessionService.saveLoginHistory(username, ip, userAgent, "SUCCESS");
        sessionService.completeSession(sessionId, response.getToken(), response.getRefreshToken());
        return response;
    }
}
