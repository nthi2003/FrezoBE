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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Luồng quên mật khẩu 2 bước:
 * <ol>
 *   <li>{@link #forgotPassword(String)} — sinh OTP 6 số, gửi email.</li>
 *   <li>{@link #verifyResetOtp(String, String)} — xác thực OTP theo email, đổi
 *       {@code reset_key} thành reset token dùng 1 lần và trả token cho client.</li>
 *   <li>{@link #resetPassword(String, String, String)} — đặt mật khẩu mới bằng reset token.</li>
 * </ol>
 * OTP luôn được tra theo email của chính người yêu cầu, không tra toàn bảng, để một
 * mã 6 số không thể vô tình khớp (hoặc bị dò) sang tài khoản khác.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthPasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int OTP_TTL_MINUTES = 10;
    private static final int RESET_TOKEN_TTL_MINUTES = 10;
    private static final int MIN_PASSWORD_LENGTH = 6;
    private static final int MAX_VERIFY_ATTEMPTS = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    /** Đếm số lần nhập OTP sai theo email, reset khi OTP mới được phát hành hoặc khi verify thành công. */
    private final Map<String, AttemptCounter> verifyAttempts = new ConcurrentHashMap<>();
    /** Thời điểm phát hành OTP gần nhất theo email — chặn spam gửi mail. */
    private final Map<String, LocalDateTime> lastOtpSentAt = new ConcurrentHashMap<>();

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
        String normalized = normalizeEmail(email);

        LocalDateTime lastSent = lastOtpSentAt.get(normalized);
        if (lastSent != null && lastSent.plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
            throw new AuthException("Vui lòng đợi " + RESEND_COOLDOWN_SECONDS + " giây trước khi yêu cầu mã mới");
        }
        lastOtpSentAt.put(normalized, LocalDateTime.now());

        userRepository.findByEmailIgnoreCase(normalized).ifPresentOrElse(user -> {
            String otp = generateOtp();
            user.setResetKey(otp);
            user.setResetDate(LocalDateTime.now().plusMinutes(OTP_TTL_MINUTES));
            userRepository.save(user);
            verifyAttempts.remove(normalized);

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

    /**
     * Xác thực OTP của riêng email đó. Thành công thì OTP bị tiêu huỷ ngay và thay bằng
     * reset token dùng 1 lần — client phải gửi token này khi đặt mật khẩu mới.
     *
     * @return reset token (dùng cho {@link #resetPassword(String, String, String)})
     */
    public String verifyResetOtp(String email, String otp) {
        if (!StringUtils.hasText(email)) {
            throw new AuthException("Email không được để trống");
        }
        if (!StringUtils.hasText(otp)) {
            throw new AuthException("Mã OTP không được để trống");
        }
        String normalized = normalizeEmail(email);
        String candidate = otp.trim();

        AttemptCounter counter = verifyAttempts.computeIfAbsent(normalized, k -> new AttemptCounter());
        if (counter.isExhausted()) {
            throw new AuthException("Bạn đã nhập sai OTP quá " + MAX_VERIFY_ATTEMPTS
                    + " lần. Vui lòng yêu cầu mã mới.");
        }

        User user = userRepository.findByEmailIgnoreCase(normalized).orElse(null);
        // Email lạ / user chưa xin OTP: trả cùng thông báo với OTP sai để không dò được email
        if (user == null || !StringUtils.hasText(user.getResetKey()) || user.getResetDate() == null) {
            counter.increment();
            throw new AuthException("Mã OTP không hợp lệ");
        }
        if (user.getResetDate().isBefore(LocalDateTime.now())) {
            throw new AuthException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
        }
        if (!constantTimeEquals(user.getResetKey(), candidate)) {
            counter.increment();
            int remaining = Math.max(0, MAX_VERIFY_ATTEMPTS - counter.getCount());
            throw new AuthException(remaining > 0
                    ? "Mã OTP không đúng. Bạn còn " + remaining + " lần thử."
                    : "Bạn đã nhập sai OTP quá " + MAX_VERIFY_ATTEMPTS + " lần. Vui lòng yêu cầu mã mới.");
        }

        String resetToken = generateResetToken();
        user.setResetKey(resetToken);
        user.setResetDate(LocalDateTime.now().plusMinutes(RESET_TOKEN_TTL_MINUTES));
        userRepository.save(user);
        verifyAttempts.remove(normalized);

        log.info("Password reset OTP verified for user {}", user.getUserName());
        return resetToken;
    }

    /** Đặt mật khẩu mới bằng reset token đã cấp ở bước verify OTP. */
    public void resetPassword(String email, String resetToken, String newPassword) {
        if (!StringUtils.hasText(email)) {
            throw new AuthException("Email không được để trống");
        }
        if (!StringUtils.hasText(resetToken)) {
            throw new AuthException("Phiên đặt lại mật khẩu không hợp lệ. Vui lòng xác thực OTP lại.");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new AuthException("Mật khẩu mới phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự");
        }

        String normalized = normalizeEmail(email);
        User user = userRepository.findByEmailIgnoreCase(normalized)
                .orElseThrow(() -> new AuthException("Phiên đặt lại mật khẩu không hợp lệ. Vui lòng xác thực OTP lại."));

        if (!StringUtils.hasText(user.getResetKey()) || !constantTimeEquals(user.getResetKey(), resetToken.trim())) {
            throw new AuthException("Phiên đặt lại mật khẩu không hợp lệ. Vui lòng xác thực OTP lại.");
        }
        if (user.getResetDate() == null || user.getResetDate().isBefore(LocalDateTime.now())) {
            throw new AuthException("Phiên đặt lại mật khẩu đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetKey(null);
        user.setResetDate(null);
        userRepository.save(user);
        lastOtpSentAt.remove(normalized);

        log.info("Password reset successful for user {}", user.getUserName());
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private static String generateOtp() {
        return String.valueOf(100_000 + SECURE_RANDOM.nextInt(900_000));
    }

    /** Token 20 hex ký tự — vừa với cột {@code reset_key} (length = 20). */
    private static String generateResetToken() {
        byte[] bytes = new byte[10];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

    /** Bộ đếm số lần nhập sai, tự hết hiệu lực sau khi OTP hết hạn. */
    private static final class AttemptCounter {
        private int count;
        private LocalDateTime firstAttemptAt = LocalDateTime.now();

        void increment() {
            resetIfStale();
            count++;
        }

        int getCount() {
            resetIfStale();
            return count;
        }

        boolean isExhausted() {
            return getCount() >= MAX_VERIFY_ATTEMPTS;
        }

        private void resetIfStale() {
            if (firstAttemptAt.plusMinutes(OTP_TTL_MINUTES).isBefore(LocalDateTime.now())) {
                count = 0;
                firstAttemptAt = LocalDateTime.now();
            }
        }
    }
}
