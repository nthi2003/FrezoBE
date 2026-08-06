package com.frezo.auth.service.impl.auth;

import com.frezo.auth.config.CustomUserDetail;
import com.frezo.auth.config.UserDetailService;
import com.frezo.auth.dto.response.LoginResponse;
import com.frezo.auth.entity.User;
import com.frezo.auth.security.JwtTokenProvider;
import com.frezo.common.exception.AuthException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Tạo JWT (access + refresh) cho user — bao gồm cả resolve role name / orgId / isAdmin
 * qua reflection để module-auth không cần compile-time dep lên qtht/qlns.
 * <p>
 * Tách khỏi {@code AuthServiceImpl} để giảm dep count và cô lập vùng reflection risky.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthTokenBuilder {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailService userDetailsService;
    private final ApplicationContext applicationContext;

    /** Sinh cặp token (access + refresh) từ {@link CustomUserDetail}. */
    public LoginResponse buildTokens(CustomUserDetail userDetail, String message) {
        return buildTokens(userDetail, message, null);
    }

    public LoginResponse buildTokens(CustomUserDetail userDetail, String message, String sessionId) {
        String token = buildAccessToken(userDetail, sessionId);
        String refreshToken = tokenProvider.generateRefreshToken(userDetail.getUsername(), sessionId);
        return LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .message(message)
                .build();
    }

    /** Sinh cặp token mới từ username — dùng cho verifyOtp + refreshToken. */
    public LoginResponse buildTokensByUsername(String username, String message) {
        return buildTokensByUsername(username, message, null);
    }

    public LoginResponse buildTokensByUsername(String username, String message, String sessionId) {
        CustomUserDetail detail = (CustomUserDetail) userDetailsService.loadUserByUsername(username);
        return buildTokens(detail, message, sessionId);
    }

    /**
     * Refresh access token — giữ nguyên refresh token, embed sessionId vào access token mới.
     */
    public LoginResponse refreshTokens(String refreshToken, String sessionId) {
        if (refreshToken == null || !tokenProvider.validateToken(refreshToken)) {
            throw new AuthException("Invalid refresh token");
        }
        String username = tokenProvider.getUsernameFromJWT(refreshToken);
        CustomUserDetail detail = (CustomUserDetail) userDetailsService.loadUserByUsername(username);
        String accessToken = buildAccessToken(detail, sessionId);
        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .message("Token refreshed")
                .build();
    }

    /**
     * Chỉ sinh access token (không refresh) — hiếm dùng, giữ để backward-compat.
     */
    public String buildAccessToken(CustomUserDetail userDetail) {
        return buildAccessToken(userDetail, null);
    }

    public String buildAccessToken(CustomUserDetail userDetail, String sessionId) {
        User user = userDetail.getUser();
        List<String> roleIds = userDetail.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());

        List<String> roles;
        String appCode = "QTHT";

        try {
            Class<?> roleRepoClass = Class.forName("com.frezo.qtht.repository.RoleRepository");
            Object roleRepo = applicationContext.getBean(roleRepoClass);
            Method findAllById = roleRepoClass.getMethod("findAllById", List.class);
            List<?> roleList = (List<?>) findAllById.invoke(roleRepo, roleIds);

            roles = roleList.stream()
                    .map(this::extractRoleName)
                    .filter(name -> name != null && !name.isEmpty())
                    .collect(Collectors.toList());

            appCode = firstAppCode(roleList, appCode);
        } catch (Exception e) {
            log.warn("[Auth] Failed to resolve roles via reflection, falling back to role IDs", e);
            roles = roleIds;
        }

        PersonInfo info = resolvePersonInfo(user.getPersonId());
        return tokenProvider.generateToken(user.getUserName(), roles, user.getDataAction(),
                info.orgId(), appCode, info.isAdmin(), sessionId);
    }

    // ============================================================
    // Helpers reflection
    // ============================================================

    private String extractRoleName(Object role) {
        try {
            Method getName = role.getClass().getMethod("getName");
            String name = (String) getName.invoke(role);
            if (name == null || name.isEmpty()) {
                Method getCode = role.getClass().getMethod("getCode");
                name = (String) getCode.invoke(role);
            }
            return name;
        } catch (Exception ex) {
            return null;
        }
    }

    private String firstAppCode(List<?> roleList, String fallback) {
        for (Object role : roleList) {
            try {
                Method getAppCode = role.getClass().getMethod("getAppCode");
                String code = (String) getAppCode.invoke(role);
                if (code != null && !code.isEmpty()) return code;
            } catch (Exception e) {
                log.warn("[Auth] Failed to extract appCode from role", e);
            }
        }
        return fallback;
    }

    /** {@code isAdmin} + {@code orgId} của Person gắn với user, lookup qua reflection. */
    private PersonInfo resolvePersonInfo(String personId) {
        if (personId == null) return new PersonInfo(false, null);
        try {
            Class<?> personRepoClass = Class.forName("com.frezo.qtht.repository.PersonRepository");
            Object personRepo = applicationContext.getBean(personRepoClass);
            Method findById = personRepoClass.getMethod("findById", Object.class);
            Optional<?> personOpt = (Optional<?>) findById.invoke(personRepo, personId);
            if (personOpt.isPresent()) {
                Object person = personOpt.get();
                boolean isAdmin = Boolean.TRUE.equals(person.getClass().getMethod("getIsAdmin").invoke(person));
                String orgId = (String) person.getClass().getMethod("getOrgId").invoke(person);
                return new PersonInfo(isAdmin, orgId);
            }
        } catch (Exception e) {
            log.warn("[Auth] Failed to resolve person info via reflection", e);
        }
        return new PersonInfo(false, null);
    }

    private record PersonInfo(boolean isAdmin, String orgId) {}
}
