package com.frezo.auth.service.impl;

import com.frezo.auth.config.CustomUserDetail;
import com.frezo.auth.config.UserDetailService;
import com.frezo.auth.dto.request.LoginRequest;
import com.frezo.auth.dto.response.LoginResponse;
import com.frezo.auth.entity.LoginHistory;
import com.frezo.auth.entity.User;
import com.frezo.auth.repository.LoginHistoryRepository;
import com.frezo.auth.repository.TokenBlacklistRepository;
import com.frezo.auth.repository.UserRepository;
import com.frezo.auth.repository.UserSessionRepository;
import com.frezo.auth.security.JwtTokenProvider;
import com.frezo.auth.service.AuthService;
import com.frezo.common.exception.AuthException;

import com.frezo.common.constant.BlockReason;
import com.frezo.common.exception.QTHTException;
import com.frezo.common.service.IpBlockService;
import com.frezo.common.service.MinioService;
import com.frezo.common.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.File;
import java.security.SecureRandom;

import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;
    private final ApplicationContext applicationContext;
    private final IpBlockService ipBlockService;
    private final HttpServletRequest httpServletRequest;
    private final UserRepository userRepository;
    private final LoginHistoryRepository loginHistoryRepository;
    private final NotificationService notificationService;
    private final UserDetailService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final UserSessionRepository userSessionRepository;
    private final MinioService minioService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public LoginResponse login(LoginRequest request) {
        String ip = resolveClientIp(httpServletRequest);
        String username = request.getUsername();
        String userAgent = httpServletRequest.getHeader("User-Agent");

        ipBlockService.checkIpBlocked(ip, username);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );

            ipBlockService.clearFailedAttempts(ip, username);
            CustomUserDetail customUserDetail = (CustomUserDetail) authentication.getPrincipal();
            User user = customUserDetail.getUser();

            // Check for 2FA
            if (Boolean.TRUE.equals(user.getRequiresTwoFactor())) {
                int otpValue = 100000 + secureRandom.nextInt(900000);
                String otp = String.valueOf(otpValue);
                user.setOtpCode(otp);
                user.setOtpExpiration(java.time.LocalDateTime.now().plusMinutes(5));
                userRepository.save(user);

                notificationService.notifyUserWithEmailFallback(username,
                        "Mã xác thực 2FA",
                        "Mã OTP của bạn là: " + otp + ". Hiệu lực trong 5 phút.",
                        true);

                saveLoginHistory(username, ip, userAgent, "2FA_REQUIRED");

                return com.frezo.auth.dto.response.LoginResponse.builder()
                        .requiresTwoFactor(true)
                        .message("Yêu cầu mã xác thực 2FA")
                        .build();
            }

            String token = generateTokenForUser(customUserDetail);
            String refreshToken = tokenProvider.generateRefreshToken(username);
            saveLoginHistory(username, ip, userAgent, "SUCCESS");

            return com.frezo.auth.dto.response.LoginResponse.builder()
                    .token(token)
                    .refreshToken(refreshToken)
                    .message("Đăng nhập thành công")
                    .build();

        } catch (BadCredentialsException | UsernameNotFoundException e) {
            saveLoginHistory(username, ip, userAgent, "FAILED");
            ipBlockService.handleFailedAttempt(ip, username, BlockReason.WRONG_PASSWORD);
            throw new AuthException("Username or password is incorrect");
        } catch (Exception e) {
            log.error("[Auth] Unexpected error", e);
            throw new RuntimeException("Internal server error: " + e.getMessage());
        }
    }

    @Override
    public com.frezo.auth.dto.response.LoginResponse verifyOtp(String username, String code) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new AuthException("User not found"));

        if (user.getOtpCode() == null || !user.getOtpCode().equals(code) ||
                user.getOtpExpiration().isBefore(java.time.LocalDateTime.now())) {
            throw new AuthException("Mã OTP không chính xác hoặc đã hết hạn");
        }

        // Clear OTP
        user.setOtpCode(null);
        user.setOtpExpiration(null);
        userRepository.save(user);

        // Generate Token
        CustomUserDetail customUserDetail = (CustomUserDetail) userDetailsService.loadUserByUsername(username);
        String token = generateTokenForUser(customUserDetail);
        String refreshToken = tokenProvider.generateRefreshToken(username);

        return com.frezo.auth.dto.response.LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .message("Xác thực thành công")
                .build();
    }

    @Override
    public void forgotPassword(String email) {
        User user = userRepository.findAll().stream()
                .filter(u -> email.equals(u.getEmail()))
                .findFirst()
                .orElseThrow(() -> new AuthException("Email không tồn tại"));

        String resetKey = UUID.randomUUID().toString().replace("-", "");
        user.setResetKey(resetKey);
        user.setResetDate(java.time.LocalDateTime.now().plusHours(1));
        userRepository.save(user);

        notificationService.notifyUserWithEmailFallback(user.getUserName(),
                "Khôi phục mật khẩu",
                "Mã khôi phục mật khẩu của bạn là: " + resetKey + ". Hiệu lực trong 1 giờ.",
                true);
    }

    @Override
    public void resetPassword(String key, String newPassword) {
        User user = userRepository.findAll().stream()
                .filter(u -> key.equals(u.getResetKey()))
                .findFirst()
                .orElseThrow(() -> new AuthException("Mã khôi phục không hợp lệ"));

        if (user.getResetDate().isBefore(LocalDateTime.now())) {
            throw new AuthException("Mã khôi phục đã hết hạn");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetKey(null);
        user.setResetDate(null);
        userRepository.save(user);
    }

    private String generateTokenForUser(CustomUserDetail customUserDetail) {
        User user = customUserDetail.getUser();
        List<String> roleIds = customUserDetail.getAuthorities().stream()
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
                    .map(role -> {
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
                    })
                    .filter(name -> name != null && !name.isEmpty())
                    .collect(Collectors.toList());

            for (Object role : roleList) {
                try {
                    Method getAppCode = role.getClass().getMethod("getAppCode");
                    String code = (String) getAppCode.invoke(role);
                    if (code != null && !code.isEmpty()) {
                        appCode = code;
                        break;
                    }
                } catch (Exception e) {
                    log.warn("[Auth] Failed to extract appCode from role", e);
                }
            }
        } catch (Exception e) {
            log.warn("[Auth] Failed to resolve roles via reflection, falling back to role IDs", e);
            roles = roleIds;
        }

        boolean isAdmin = false;
        String orgId = null;
        try {
            Class<?> personRepoClass = Class.forName("com.frezo.qtht.repository.PersonRepository");
            Object personRepo = applicationContext.getBean(personRepoClass);
            if (user.getPersonId() != null) {
                Method findById = personRepoClass.getMethod("findById", Object.class);
               Optional<?> personOpt = (Optional<?>) findById.invoke(personRepo, user.getPersonId());
                if (personOpt.isPresent()) {
                    Object person = personOpt.get();
                    isAdmin = Boolean.TRUE.equals(person.getClass().getMethod("getIsAdmin").invoke(person));
                    orgId = (String) person.getClass().getMethod("getOrgId").invoke(person);
                }
            }
        } catch (Exception e) {
            log.warn("[Auth] Failed to resolve person info via reflection", e);
        }

        return tokenProvider.generateToken(user.getUserName(), roles, user.getDataAction(), orgId, appCode, isAdmin);
    }

    private void saveLoginHistory(String username, String ip, String userAgent, String status) {
        LoginHistory history = LoginHistory.builder()
                .userName(username)
                .ipAddress(ip)
                .userAgent(userAgent)
                .loginTime(java.time.LocalDateTime.now())
                .status(status)
                .build();
        loginHistoryRepository.save(history);
    }

    @Override
    public List<LoginHistory> getLoginHistory(String username) {
        return loginHistoryRepository.findByUserNameOrderByLoginTimeDesc(username);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // Handle multiple IPs in X-Forwarded-For (take the first one)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        if (refreshToken == null || !tokenProvider.validateToken(refreshToken)) {
            throw new AuthException("Invalid refresh token");
        }

        String username = tokenProvider.getUsernameFromJWT(refreshToken);
        CustomUserDetail customUserDetail = (CustomUserDetail) userDetailsService.loadUserByUsername(username);

        String newToken = generateTokenForUser(customUserDetail);
        String newRefreshToken = tokenProvider.generateRefreshToken(username);

        return LoginResponse.builder()
                .token(newToken)
                .refreshToken(newRefreshToken)
                .message("Token refreshed")
                .build();
    }
    @Override
    public String upload(File file, String username) {
        String extension = getExtension(file.getName());
        String objectName = username + "/avatar" + extension;
        return minioService.uploadFileFromPath(objectName, file);
    }

    @Override
    public String uploadAvatar(MultipartFile file, String username) {
        try {
            String extension = getExtension(file.getOriginalFilename());
            String objectName = "frezo-user/avatar/" + username + extension;

            String url = minioService.uploadFile(objectName, file);

            // Save URL to user record
            User user = userRepository.findByUserName(username)
                    .orElseThrow(() -> new AuthException("User not found"));
            user.setAvatarUrl(url);
            userRepository.save(user);

            log.info("Avatar uploaded for user {}: {}", username, objectName);
            return url;
        } catch (Exception e) {
            log.error("Failed to upload avatar for user {}: {}", username, e.getMessage(), e);
            throw new RuntimeException("Không thể tải ảnh đại diện: " + e.getMessage());
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".png";
        return filename.substring(filename.lastIndexOf("."));
    }

    @Override
    public void logout(String token, String username) {
        if (token == null || token.isEmpty()) {
            throw new AuthException("Token is required");
        }

        // Add token to blacklist
        com.frezo.auth.entity.TokenBlacklist blacklist = com.frezo.auth.entity.TokenBlacklist.builder()
                .token(token)
                .username(username)
                .blacklistedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();
        tokenBlacklistRepository.save(blacklist);

        // Deactivate session
        userSessionRepository.findByToken(token).ifPresent(session -> {
            session.setIsActive(false);
            session.setRevokedAt(LocalDateTime.now());
            session.setRevokedBy(username);
            userSessionRepository.save(session);
        });

        log.info("User {} logged out successfully", username);
    }

    private void createUserSession(String username, String token, String refreshToken, String ip, String userAgent) {
        try {
            com.frezo.auth.entity.UserSession session = com.frezo.auth.entity.UserSession.builder()
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

    private String parseDeviceInfo(String userAgent) {
        if (userAgent == null) return "Unknown";
        if (userAgent.contains("Mobile")) return "Mobile";
        if (userAgent.contains("Tablet")) return "Tablet";
        return "Desktop";
    }

    @Override
    public Object getProfile() {
        String username = com.frezo.common.helper.SystemUtils.getCurrentUsername();
        if (username == null) {
            throw new AuthException("User not authenticated");
        }

        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new AuthException("User not found"));

        java.util.Map<String, Object> profile = new java.util.HashMap<>();
        profile.put("id", user.getId());
        profile.put("username", user.getUserName());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("avatarUrl", user.getAvatarUrl());
        profile.put("dataAction", user.getDataAction());

        // Get Person info via reflection to avoid dependency
        if (user.getPersonId() != null) {
            try {
                Class<?> personRepoClass = Class.forName("PersonRepository");
                Object personRepo = applicationContext.getBean(personRepoClass);
                Method findById = personRepoClass.getMethod("findById", Object.class);
                Optional<?> personOpt = (Optional<?>) findById.invoke(personRepo, user.getPersonId());
                if (personOpt.isPresent()) {
                    Object person = personOpt.get();
                    profile.put("personId", user.getPersonId());
                    profile.put("phone", person.getClass().getMethod("getPhone").invoke(person));
                    profile.put("jobTitle", person.getClass().getMethod("getJobTitle").invoke(person));
                    profile.put("orgId", person.getClass().getMethod("getOrgId").invoke(person));
                    profile.put("isAdmin", person.getClass().getMethod("getIsAdmin").invoke(person));
                }
            } catch (Exception e) {
                log.warn("[Auth] Failed to resolve person info for profile", e);
            }
        }

        return profile;
    }
}
