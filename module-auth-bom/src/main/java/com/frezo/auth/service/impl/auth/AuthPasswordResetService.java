package com.frezo.auth.service.impl.auth;

import com.frezo.auth.entity.User;
import com.frezo.auth.repository.UserRepository;
import com.frezo.common.exception.AuthException;
import com.frezo.common.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Xử lý quên mật khẩu / reset mật khẩu qua reset key thời hạn 1 giờ.
 */
@Component
@RequiredArgsConstructor
public class AuthPasswordResetService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;

    /** Sinh reset key và gửi cho user qua Notification (email fallback). */
    public void forgotPassword(String email) {
        User user = userRepository.findAll().stream()
                .filter(u -> email.equals(u.getEmail()))
                .findFirst()
                .orElseThrow(() -> new AuthException("Email không tồn tại"));

        String resetKey = UUID.randomUUID().toString().replace("-", "");
        user.setResetKey(resetKey);
        user.setResetDate(LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        notificationService.notifyUserWithEmailFallback(user.getUserName(),
                "Khôi phục mật khẩu",
                "Mã khôi phục mật khẩu của bạn là: " + resetKey + ". Hiệu lực trong 1 giờ.",
                true);
    }

    /** Verify key + set mật khẩu mới đã hash. */
    public void resetPassword(String key, String newPassword) {
        User user = userRepository.findAll().stream()
                .filter(u -> key.equals(u.getResetKey()))
                .findFirst()
                .orElseThrow(() -> new AuthException("Mã khôi phục không hợp lệ"));

        if (user.getResetDate() == null || user.getResetDate().isBefore(LocalDateTime.now())) {
            throw new AuthException("Mã khôi phục đã hết hạn");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetKey(null);
        user.setResetDate(null);
        userRepository.save(user);
    }
}
