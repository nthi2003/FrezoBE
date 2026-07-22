package com.frezo.auth.service.impl.auth;

import com.frezo.auth.dto.response.LoginResponse;
import com.frezo.auth.entity.User;
import com.frezo.auth.repository.UserRepository;
import com.frezo.common.exception.AuthException;
import com.frezo.common.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Xử lý 2FA — sinh OTP, gửi qua NotificationService, verify khi user submit lại.
 */
@Component
@RequiredArgsConstructor
public class AuthTwoFactorService {

    /** SecureRandom là thread-safe → chia sẻ instance static an toàn. */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuthTokenBuilder tokenBuilder;

    /**
     * Bắt đầu flow 2FA: sinh OTP 6 số, lưu vào user + hết hạn 5 phút, gửi qua notification.
     * Trả về response yêu cầu FE hỏi thêm mã.
     */
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
                .orElseThrow(() -> new AuthException("User not found"));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(code)
                || user.getOtpExpiration() == null
                || user.getOtpExpiration().isBefore(LocalDateTime.now())) {
            throw new AuthException("Mã OTP không chính xác hoặc đã hết hạn");
        }

        user.setOtpCode(null);
        user.setOtpExpiration(null);
        userRepository.save(user);

        return tokenBuilder.buildTokensByUsername(username, "Xác thực thành công");
    }
}
