package com.frezo.auth.service.impl.auth;

import com.frezo.auth.entity.User;
import com.frezo.auth.repository.UserRepository;
import com.frezo.common.audit.AuditLogService;
import com.frezo.common.constant.BlockReason;
import com.frezo.common.exception.QTHTException;
import com.frezo.common.service.IpBlockService;
import com.frezo.common.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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


    private static final String OTP_PREFIX = "OTP:";
    private static final String TOKEN_PREFIX = "TOK:";
    private static final int STATUS_ACTIVE = 1;
    private static final String LOCKED_MESSAGE = "Bạn đã nhập sai OTP quá " + MAX_VERIFY_ATTEMPTS
            + " lần. Tài khoản đã bị khóa, vui lòng liên hệ quản trị viên.";


    private final Map<String, AttemptCounter> verifyAttempts = new ConcurrentHashMap<>();

    private final Map<String, LocalDateTime> lastOtpSentAt = new ConcurrentHashMap<>();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final IpBlockService ipBlockService;
    private final AuditLogService auditLogService;


    public void forgotPassword(String email) {
        if (!StringUtils.hasText(email)) {
            throw new QTHTException("email.required");
        }
        String normalized = normalizeEmail(email);

        LocalDateTime lastSent = lastOtpSentAt.get(normalized);
        if (lastSent != null && lastSent.plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
            throw new QTHTException("Vui lòng đợi " + RESEND_COOLDOWN_SECONDS + " giây trước khi yêu cầu mã mới");
        }
        lastOtpSentAt.put(normalized, LocalDateTime.now());

        userRepository.findByEmailIgnoreCase(normalized).ifPresentOrElse(user -> {
            if (user.getStatus() != null && user.getStatus() != STATUS_ACTIVE) {
                log.warn("Forgot-password requested for locked account {}", user.getUserName());
                return;
            }

            String otp = generateOtp();
            user.setResetKey(OTP_PREFIX + sha256(otp));
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
            log.info("Password reset OTP generated for user {}", user.getUserName());
        }, () -> log.info("Forgot-password requested for unknown email: {}", normalized));
    }

    public String verifyResetOtp(String email, String otp) {
        if (!StringUtils.hasText(email)) {
            throw new QTHTException("email.required");
        }
        if (!StringUtils.hasText(otp)) {
            throw new QTHTException("otp.required");
        }
        String normalized = normalizeEmail(email);
        String candidate = otp.trim();

        AttemptCounter counter = verifyAttempts.computeIfAbsent(normalized, k -> new AttemptCounter());
        if (counter.isExhausted()) {
            throw new QTHTException(LOCKED_MESSAGE);
        }

        User user = userRepository.findByEmailIgnoreCase(normalized).orElse(null);
        if (user == null || !StringUtils.hasText(user.getResetKey()) || user.getResetDate() == null) {
            counter.increment();
            throw new QTHTException("otp.invalid");
        }
        if (user.getResetDate().isBefore(LocalDateTime.now())) {
            throw new QTHTException("otp.exp");
        }
        if (!constantTimeEquals(user.getResetKey(), OTP_PREFIX + sha256(candidate))) {
            counter.increment();
            int remaining = Math.max(0, MAX_VERIFY_ATTEMPTS - counter.getCount());
            if (remaining > 0) {
                log.warn("Wrong reset OTP for user {} ({} attempts left)", user.getUserName(), remaining);
                throw new QTHTException("otp.incorrect");
            }
            lockAfterBruteForce(user, counter.getCount());
            throw new QTHTException(LOCKED_MESSAGE);
        }

        String resetToken = generateResetToken();
        user.setResetKey(TOKEN_PREFIX + sha256(resetToken));
        user.setResetDate(LocalDateTime.now().plusMinutes(RESET_TOKEN_TTL_MINUTES));
        userRepository.save(user);
        verifyAttempts.remove(normalized);

        log.info("Password reset OTP verified for user {}", user.getUserName());
        return resetToken;
    }

    private void lockAfterBruteForce(User user, int attempts) {
        user.setResetKey(null);
        user.setResetDate(null);
        userRepository.save(user);

        HttpServletRequest request = currentRequest();
        String ip = request != null ? IpResolver.resolveClientIp(request) : null;

        try {
            ipBlockService.handleFailedAttempt(ip, user.getUserName(), BlockReason.OTP_BRUTE_FORCE);
            ipBlockService.lockUserAndBlacklistIp(ip, user.getUserName(), BlockReason.OTP_BRUTE_FORCE, null);
        } catch (Exception e) {
            log.error("Failed to lock account {} after OTP brute force: {}", user.getUserName(), e.getMessage());
        }

        auditLogService.logAction("OTP_BRUTE_FORCE_LOCK", "users", user.getId(),
                "Khóa tài khoản " + user.getUserName() + " và chặn IP " + (ip != null ? ip : "?")
                        + " sau " + attempts + " lần nhập sai OTP quên mật khẩu",
                request);

        log.warn("Account {} locked after {} wrong reset OTP attempts from IP {}",
                user.getUserName(), attempts, ip);
    }


    public void resetPassword(String email, String resetToken, String newPassword) {
        if (!StringUtils.hasText(email)) {
            throw new QTHTException("email.required");
        }
        if (!StringUtils.hasText(resetToken)) {
            throw new QTHTException("Phiên đặt lại mật khẩu không hợp lệ. Vui lòng xác thực OTP lại.");
        }
        if (!StringUtils.hasText(newPassword) || newPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new QTHTException("Mật khẩu mới phải có ít nhất " + MIN_PASSWORD_LENGTH + " ký tự");
        }

        String normalized = normalizeEmail(email);
        User user = userRepository.findByEmailIgnoreCase(normalized)
                .orElseThrow(() -> new QTHTException("Phiên đặt lại mật khẩu không hợp lệ. Vui lòng xác thực OTP lại."));

        if (!StringUtils.hasText(user.getResetKey())
                || !constantTimeEquals(user.getResetKey(), TOKEN_PREFIX + sha256(resetToken.trim()))) {
            throw new QTHTException("Phiên đặt lại mật khẩu không hợp lệ. Vui lòng xác thực OTP lại.");
        }
        if (user.getResetDate() == null || user.getResetDate().isBefore(LocalDateTime.now())) {
            throw new QTHTException("Phiên đặt lại mật khẩu đã hết hạn. Vui lòng yêu cầu mã mới.");
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
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }


    private static String generateResetToken() {
        byte[] bytes = new byte[16];
        SECURE_RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }

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
