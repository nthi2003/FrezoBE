package com.frezo.auth.service.impl.auth;

import com.frezo.auth.config.CustomUserDetail;
import com.frezo.auth.dto.request.LoginRequest;
import com.frezo.auth.dto.response.LoginResponse;
import com.frezo.auth.entity.User;
import com.frezo.common.constant.BlockReason;
import com.frezo.common.exception.QTHTException;
import com.frezo.common.service.IpBlockService;
import jakarta.security.auth.message.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
@Slf4j
public class AuthLoginProcessor {

    private final AuthenticationManager authenticationManager;
    private final IpBlockService ipBlockService;
    private final AuthTokenBuilder tokenBuilder;
    private final AuthTwoFactorService twoFactorService;
    private final AuthSessionService sessionService;

    public LoginResponse login(LoginRequest request) {
        String ip = IpResolver.currentClientIp();
        String userAgent = IpResolver.currentUserAgent();
        String username = request.getUsername();

        ipBlockService.checkIpBlocked(ip, username);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword()));
            ipBlockService.clearFailedAttempts(ip, username);

            CustomUserDetail detail = (CustomUserDetail) authentication.getPrincipal();
            User user = detail.getUser();

            if (Boolean.TRUE.equals(user.getRequiresTwoFactor())) {
                LoginResponse twofa = twoFactorService.startTwoFactor(user);
                sessionService.saveLoginHistory(username, ip, userAgent, "2FA_REQUIRED");
                return twofa;
            }

            String sessionId = sessionService.beginSession(username, ip, userAgent);
            LoginResponse response = tokenBuilder.buildTokens(detail, "Đăng nhập thành công", sessionId);
            sessionService.saveLoginHistory(username, ip, userAgent, "SUCCESS");
            sessionService.completeSession(sessionId, response.getToken(), response.getRefreshToken());
            return response;

        } catch (BadCredentialsException | UsernameNotFoundException e) {
            sessionService.saveLoginHistory(username, ip, userAgent, "FAILED");
            ipBlockService.handleFailedAttempt(ip, username, BlockReason.WRONG_PASSWORD);
            throw new QTHTException("exception.auth.failed");
        } catch (Exception e) {
            log.error("[Auth] Unexpected error", e);
            throw new RuntimeException("Internal server error: " + e.getMessage());
        }
    }

    public LoginResponse refreshToken(String refreshToken) {
        sessionService.assertRefreshTokenAllowed(refreshToken);
        String sessionId = sessionService.resolveSessionId(refreshToken);
        LoginResponse response = tokenBuilder.refreshTokens(refreshToken, sessionId);
        sessionService.rotateAccessToken(refreshToken, response.getToken());
        return response;
    }
}
