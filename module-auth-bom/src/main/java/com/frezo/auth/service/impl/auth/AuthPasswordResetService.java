package com.frezo.auth.service.impl.auth;

import com.frezo.auth.entity.User;
import com.frezo.auth.repository.UserRepository;
import com.frezo.common.exception.AuthException;
import com.frezo.common.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;


@Slf4j
@Component
@RequiredArgsConstructor
public class AuthPasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_TTL_MINUTES = 10;
    private static final int MIN_PASSWORD_LENGTH = 6;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    /**
     * Sinh OTP 6 số, lưu {@code resetKey}/{@code resetDate}, gửi email.
     * Luôn trả success (không tiết lộ email có tồn tại hay không).
     */
    public void forgotPassword(String email) {
        if (!StringUtils.hasText(email)) {
            throw new AuthException("Email không được để trống");
        }
        String normalized = email.trim().toLowerCase();

        userRepository.findByEmailIgnoreCase(normalized).ifPresentOrElse(user -> {
            String otp = generateOtp();
            user.setResetKey(otp);
            user.setResetDate(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES));
            userRepository.save(user);

            String displayName = StringUtils.hasText(user.getName()) ? user.getName() : user.getUserName();
            String message = "Xin chào " + displayName + ",\n\n"
                    + "Mã OTP khôi phục mật khẩu Frezo của bạn là: " + otp + "\n"
                    + "Mã có hiệu lực trong " + OTP_TTL_MINUTES + " phút. Không chia sẻ mã này với ai.\n\n"
                    + "Nếu bạn không yêu cầu, hãy bỏ qua email này.";

            try {
                notificationService.notifyUserWithEmailFallback(
                        user.getUserName(),
                        "Mã OTP khôi phục mật khẩu",
                        message,
                        true);
            } catch (Exception e) {
                log.error("Failed to send password-reset OTP email to {}: {}", user.getEmail(), e.getMessage());
            }
            // Dev: OTP chỉ log ở DEBUG — production không lộ mã
            log.info("Password reset OTP generated for user {}", user.getUserName());
            log.debug("Password reset OTP for {} ({}): {}", user.getUserName(), user.getEmail(), otp);
        }, () -> log.info("Forgot-password requested for unknown email: {}", normalized));
    }

    /** Verify OTP ({@code key}) + set mật khẩu mới đã hash. */
    public void resetPassword(String key, String newPassword) {
        if (!StringUtils.hasText(key)) {
            throw new AuthException("Mã OTP không được để trống");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new AuthException("Mật khẩu mới phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự");
        }

        String otp = key.trim();
        User user = userRepository.findByResetKey(otp)
                .orElseThrow(() -> new AuthException("Mã OTP không hợp lệ"));

        if (user.getResetDate() == null || user.getResetDate().isBefore(LocalDateTime.now())) {
            throw new AuthException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetKey(null);
        user.setResetDate(null);
        userRepository.save(user);

        log.info("Password reset successful for user {}", user.getUserName());
    }

    private static String generateOtp() {
        return String.valueOf(100_000 + SECURE_RANDOM.nextInt(900_000));
    }
}
