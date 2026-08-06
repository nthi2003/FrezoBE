package com.frezo.auth.service.impl.auth;

import com.frezo.auth.entity.LoginHistory;
import com.frezo.auth.entity.TokenBlacklist;
import com.frezo.auth.entity.UserSession;
import com.frezo.auth.repository.LoginHistoryRepository;
import com.frezo.auth.repository.TokenBlacklistRepository;
import com.frezo.auth.repository.UserSessionRepository;
import com.frezo.auth.security.JwtTokenProvider;
import com.frezo.common.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Quản lý phiên đăng nhập & blacklist token & lịch sử login.
 * <p>
 * Gom 3 concern rất liên quan (session/blacklist/history) vào 1 component để giảm
 * số dependency của {@code AuthServiceImpl} — vẫn ≤ 5 deps.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthSessionService {

    private final UserSessionRepository userSessionRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final JwtTokenProvider tokenProvider;

    /** Ghi lịch sử login (SUCCESS / FAILED / 2FA_REQUIRED). */
    public void saveLoginHistory(String username, String ip, String userAgent, String status) {
        LoginHistory history = LoginHistory.builder()
                .userName(username)
                .ipAddress(ip)
                .userAgent(userAgent)
                .loginTime(LocalDateTime.now())
                .status(status)
                .build();
        loginHistoryRepository.save(history);
    }

    public List<LoginHistory> getRecentLoginHistory(String username) {
        return loginHistoryRepository.findTop3ByUserNameOrderByLoginTimeDesc(username);
    }

    /**
     * Logout — blacklist token + deactivate session tương ứng.
     */
    public void logout(String token, String username) {
        if (token == null || token.isEmpty()) {
            throw new AuthException("Token is required");
        }
        findSessionForToken(token).ifPresentOrElse(
                session -> revokeSessionRecord(session, username),
                () -> blacklistToken(token, username));
        log.info("User {} logged out successfully", username);
    }

    /**
     * Admin / user revoke — deactivate session và blacklist cả access + refresh token
     * để JWT vẫn còn hạn cũng bị từ chối ngay ở filter.
     */
    public void revokeSessionRecord(UserSession session, String revokedBy) {
        session.setIsActive(false);
        session.setRevokedAt(LocalDateTime.now());
        session.setRevokedBy(revokedBy);
        userSessionRepository.save(session);

        blacklistToken(session.getToken(), session.getUsername());
        blacklistToken(session.getRefreshToken(), session.getUsername());

        log.info("Session {} revoked by {}", session.getId(), revokedBy);
    }

    /** Chặn refresh khi token bị blacklist hoặc session đã thu hồi. */
    public void assertRefreshTokenAllowed(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthException("Invalid refresh token");
        }
        if (tokenBlacklistRepository.existsByToken(refreshToken)) {
            throw new AuthException("Invalid refresh token");
        }

        String sessionId = tokenProvider.getSessionIdFromJWT(refreshToken);
        if (StringUtils.hasText(sessionId)) {
            UserSession session = userSessionRepository.findById(sessionId)
                    .orElseThrow(() -> new AuthException("Session revoked"));
            if (!Boolean.TRUE.equals(session.getIsActive())) {
                throw new AuthException("Session revoked");
            }
            return;
        }

        userSessionRepository.findByRefreshToken(refreshToken).ifPresent(session -> {
            if (!Boolean.TRUE.equals(session.getIsActive())) {
                throw new AuthException("Session revoked");
            }
        });
    }

    /** Resolve sessionId từ refresh JWT claim hoặc DB lookup (legacy token). */
    public String resolveSessionId(String refreshToken) {
        String sessionId = tokenProvider.getSessionIdFromJWT(refreshToken);
        if (StringUtils.hasText(sessionId)) {
            return sessionId;
        }
        return userSessionRepository.findByRefreshToken(refreshToken)
                .map(UserSession::getId)
                .orElse(null);
    }

    private void blacklistToken(String token, String username) {
        if (token == null || token.isBlank() || username == null || username.isBlank()) {
            return;
        }
        if (tokenBlacklistRepository.existsByToken(token)) {
            return;
        }
        tokenBlacklistRepository.save(TokenBlacklist.builder()
                .token(token)
                .username(username)
                .blacklistedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build());
    }

    /**
     * Tạo session stub trước khi sinh JWT — trả {@code sessionId} để embed vào access/refresh token.
     */
    public String beginSession(String username, String ip, String userAgent) {
        try {
            UserSession session = UserSession.builder()
                    .username(username)
                    .ipAddress(ip)
                    .userAgent(userAgent)
                    .deviceInfo(parseDeviceInfo(userAgent))
                    .loginTime(LocalDateTime.now())
                    .lastActiveTime(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .isActive(true)
                    .build();
            userSessionRepository.save(session);
            return session.getId();
        } catch (Exception e) {
            log.warn("Failed to begin session for {}", username, e);
            return null;
        }
    }

    /** Gắn access/refresh token vào session sau login. */
    public void completeSession(String sessionId, String token, String refreshToken) {
        if (!StringUtils.hasText(sessionId)) {
            return;
        }
        try {
            userSessionRepository.findById(sessionId).ifPresent(session -> {
                session.setToken(token);
                session.setRefreshToken(refreshToken);
                session.setLastActiveTime(LocalDateTime.now());
                userSessionRepository.save(session);
            });
        } catch (Exception e) {
            log.warn("Failed to complete session {}", sessionId, e);
        }
    }

    /** @deprecated Dùng {@link #beginSession} + {@link #completeSession} để JWT có sessionId claim. */
    @Deprecated
    public void createSession(String username, String token, String refreshToken,
                              String ip, String userAgent) {
        try {
            UserSession session = UserSession.builder()
                    .username(username)
                    .token(token)
                    .refreshToken(refreshToken)
                    .ipAddress(ip)
                    .userAgent(userAgent)
                    .deviceInfo(parseDeviceInfo(userAgent))
                    .loginTime(LocalDateTime.now())
                    .lastActiveTime(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .isActive(true)
                    .build();
            userSessionRepository.save(session);
        } catch (Exception e) {
            log.warn("Failed to create user session for {}", username, e);
        }
    }

    /**
     * Sau refresh JWT: cập nhật access token trên session còn active (khớp heartbeat).
     * FE giữ nguyên refresh token cũ — tra theo refreshToken.
     */
    public void rotateAccessToken(String refreshToken, String newAccessToken) {
        if (refreshToken == null || refreshToken.isBlank() || newAccessToken == null || newAccessToken.isBlank()) {
            return;
        }
        try {
            userSessionRepository.findByRefreshToken(refreshToken).ifPresent(session -> {
                if (!Boolean.TRUE.equals(session.getIsActive())) return;
                session.setToken(newAccessToken);
                session.setLastActiveTime(LocalDateTime.now());
                userSessionRepository.save(session);
            });
        } catch (Exception e) {
            log.warn("Failed to rotate session access token", e);
        }
    }

    private String parseDeviceInfo(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Mobile")) return "Mobile";
        if (userAgent.contains("Tablet")) return "Tablet";
        return "Desktop";
    }

    private Optional<UserSession> findSessionForToken(String token) {
        Optional<UserSession> byToken = userSessionRepository.findByToken(token);
        if (byToken.isPresent()) {
            return byToken;
        }
        String sessionId = tokenProvider.getSessionIdFromJWT(token);
        if (StringUtils.hasText(sessionId)) {
            return userSessionRepository.findById(sessionId);
        }
        return Optional.empty();
    }
}
