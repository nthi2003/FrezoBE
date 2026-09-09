package com.frezo.auth.service;

import com.frezo.auth.entity.UserSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserSessionService {

    List<UserSession> getActiveSessions(String username);

    Page<UserSession> getActiveSessions(String username, Pageable pageable);

    void revokeSession(String sessionId, String revokedBy);

    int revokeSessions(List<String> sessionIds, String revokedBy);

    void revokeAllOtherSessions(String username, String currentSessionId, String revokedBy);

    long countActiveSessions(String username);

    boolean heartbeat(String token, String sessionId, String username);


    long countAllActiveSessions();

    long countOnlineUsers(int onlineSeconds);

    Page<UserSession> getAllActiveSessions(Pageable pageable);
}
