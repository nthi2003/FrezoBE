package com.frezo.auth.security;

import com.frezo.auth.repository.TokenBlacklistRepository;
import com.frezo.auth.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;


@Service
@RequiredArgsConstructor
public class SessionValidationService {

    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final UserSessionRepository userSessionRepository;
    private final JwtTokenProvider tokenProvider;

    public boolean isAccessTokenRejected(String jwt) {
        if (!StringUtils.hasText(jwt)) {
            return false;
        }
        if (tokenBlacklistRepository.existsByToken(jwt)) {
            return true;
        }

        String sessionId = tokenProvider.getSessionIdFromJWT(jwt);
        if (StringUtils.hasText(sessionId)) {
            return userSessionRepository.findById(sessionId)
                    .map(session -> !Boolean.TRUE.equals(session.getIsActive()))
                    .orElse(true);
        }

        return userSessionRepository.findByToken(jwt)
                .map(session -> !Boolean.TRUE.equals(session.getIsActive()))
                .orElse(false);
    }
}
