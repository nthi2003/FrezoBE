package com.frezo.auth.service;

import com.frezo.auth.entity.UserSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserSessionService {

    List<UserSession> getActiveSessions(String username);

    Page<UserSession> getActiveSessions(String username, Pageable pageable);

    void revokeSession(String sessionId, String revokedBy);

    void revokeAllOtherSessions(String username, String currentSessionId, String revokedBy);

    long countActiveSessions(String username);

    /** Cập nhật lastActiveTime theo JWT token hiện tại. */
    boolean heartbeat(String token);

    /** Số phiên active toàn hệ thống. */
    long countAllActiveSessions();

    /** Số user distinct còn heartbeat trong cửa sổ onlineMinutes. */
    long countOnlineUsers(int onlineMinutes);

    Page<UserSession> getAllActiveSessions(Pageable pageable);
}
