package com.frezo.auth.service.impl;

import com.frezo.auth.entity.UserSession;
import com.frezo.auth.repository.UserSessionRepository;
import com.frezo.auth.service.UserSessionService;
import com.frezo.auth.service.impl.auth.AuthSessionService;
import com.frezo.common.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionServiceImpl implements UserSessionService {

    private final UserSessionRepository userSessionRepository;
    private final AuthSessionService authSessionService;

    @Override
    public List<UserSession> getActiveSessions(String username) {
        return userSessionRepository.findByUsernameAndIsActiveTrue(username);
    }

    @Override
    public Page<UserSession> getActiveSessions(String username, Pageable pageable) {
        return userSessionRepository.findByUsernameAndIsActiveTrue(username, pageable);
    }

    @Override
    @Transactional
    public void revokeSession(String sessionId, String revokedBy) {
        UserSession session = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> new AuthException("Session not found"));
        authSessionService.revokeSessionRecord(session, revokedBy);
    }

    @Override
    @Transactional
    public void revokeAllOtherSessions(String username, String currentSessionId, String revokedBy) {
        List<UserSession> otherSessions = userSessionRepository.findByUsernameAndIsActiveTrueAndIdNot(username, currentSessionId);

        for (UserSession session : otherSessions) {
            authSessionService.revokeSessionRecord(session, revokedBy);
        }
        log.info("Revoked {} other sessions for user {}", otherSessions.size(), username);
    }

    @Override
    public long countActiveSessions(String username) {
        return userSessionRepository.countByUsernameAndIsActiveTrue(username);
    }

    @Override
    @Transactional
    public boolean heartbeat(String token, String sessionId, String username) {
        LocalDateTime now = LocalDateTime.now();
        String trimmed = StringUtils.hasText(token) ? token.trim() : null;

        if (StringUtils.hasText(sessionId)) {
            int bySession = userSessionRepository.touchBySessionId(sessionId, now);
            if (bySession > 0) return true;
        }

        if (trimmed != null) {
            int byToken = userSessionRepository.touchByToken(trimmed, now);
            if (byToken > 0) return true;
        }

        if (StringUtils.hasText(username)) {
            int byUser = userSessionRepository.touchByUsername(username, now);
            if (byUser > 0) {
                if (trimmed != null) {
                    userSessionRepository.updateTokenByUsername(username, trimmed);
                }
                return true;
            }
            // Self-heal: login cũ chưa ghi UserSession → tạo phiên để online-count hoạt động
            if (trimmed != null) {
                UserSession session = UserSession.builder()
                        .username(username)
                        .token(trimmed)
                        .loginTime(now)
                        .lastActiveTime(now)
                        .expiresAt(now.plusHours(24))
                        .isActive(true)
                        .deviceInfo("Desktop")
                        .build();
                userSessionRepository.save(session);
                return true;
            }
        }
        return false;
    }

    @Override
    public long countAllActiveSessions() {
        return userSessionRepository.countByIsActiveTrue();
    }

    @Override
    public long countOnlineUsers(int onlineSeconds) {
        int seconds = onlineSeconds <= 0 ? 90 : Math.min(onlineSeconds, 3600);
        return userSessionRepository.countDistinctOnlineUsers(LocalDateTime.now().minusSeconds(seconds));
    }

    @Override
    public Page<UserSession> getAllActiveSessions(Pageable pageable) {
        return userSessionRepository.findByIsActiveTrue(pageable);
    }
}
