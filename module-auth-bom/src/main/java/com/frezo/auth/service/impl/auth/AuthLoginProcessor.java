package com.frezo.auth.service.impl.auth;

import com.frezo.auth.config.CustomUserDetail;
import com.frezo.auth.dto.request.LoginRequest;
import com.frezo.auth.dto.response.LoginResponse;
import com.frezo.auth.entity.User;
import com.frezo.common.constant.BlockReason;
import com.frezo.common.exception.AuthException;
import com.frezo.common.service.IpBlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Xử lý flow đăng nhập chuẩn: check IP blacklist → authenticate → nếu 2FA thì trigger OTP,
 * ngược lại sinh cặp token và ghi lịch sử.
 * <p>
 * Không inject {@link jakarta.servlet.http.HttpServletRequest} trực tiếp — lấy IP qua
 * {@link IpResolver#currentClientIp()} để tránh đội thêm dependency.
 */
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

            LoginResponse response = tokenBuilder.buildTokens(detail, "Đăng nhập thành công");
            sessionService.saveLoginHistory(username, ip, userAgent, "SUCCESS");
            return response;

        } catch (BadCredentialsException | UsernameNotFoundException e) {
            sessionService.saveLoginHistory(username, ip, userAgent, "FAILED");
            ipBlockService.handleFailedAttempt(ip, username, BlockReason.WRONG_PASSWORD);
            // GlobalExceptionHandler dịch i18n theo Accept-Language
            throw new AuthException("exception.auth.failed");
        } catch (Exception e) {
            log.error("[Auth] Unexpected error", e);
            throw new RuntimeException("Internal server error: " + e.getMessage());
        }
    }

    /** Refresh cặp token — delegate xuống {@link AuthTokenBuilder}. */
    public LoginResponse refreshToken(String refreshToken) {
        return tokenBuilder.refreshTokens(refreshToken);
    }
}
