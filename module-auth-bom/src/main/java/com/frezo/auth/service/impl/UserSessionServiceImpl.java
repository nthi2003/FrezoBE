package com.frezo.auth.service.impl;

import com.frezo.auth.entity.UserSession;
import com.frezo.auth.repository.UserSessionRepository;
import com.frezo.auth.service.UserSessionService;
import com.frezo.common.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionServiceImpl implements UserSessionService {

    private final UserSessionRepository userSessionRepository;

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

        session.setIsActive(false);
        session.setRevokedAt(LocalDateTime.now());
        session.setRevokedBy(revokedBy);
        userSessionRepository.save(session);

        log.info("Session {} revoked by {}", sessionId, revokedBy);
    }

    @Override
    @Transactional
    public void revokeAllOtherSessions(String username, String currentSessionId, String revokedBy) {
        List<UserSession> otherSessions = userSessionRepository.findByUsernameAndIsActiveTrueAndIdNot(username, currentSessionId);

        for (UserSession session : otherSessions) {
            session.setIsActive(false);
            session.setRevokedAt(LocalDateTime.now());
            session.setRevokedBy(revokedBy);
        }

        userSessionRepository.saveAll(otherSessions);
        log.info("Revoked {} other sessions for user {}", otherSessions.size(), username);
    }

    @Override
    public long countActiveSessions(String username) {
        return userSessionRepository.countByUsernameAndIsActiveTrue(username);
    }
}

